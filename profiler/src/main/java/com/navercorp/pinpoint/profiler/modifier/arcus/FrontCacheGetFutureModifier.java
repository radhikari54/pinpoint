/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.modifier.arcus;

import com.navercorp.pinpoint.bootstrap.Agent;
import com.navercorp.pinpoint.bootstrap.instrument.ByteCodeInstrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.interceptor.Interceptor;
import com.navercorp.pinpoint.bootstrap.interceptor.SimpleAroundInterceptor;
import com.navercorp.pinpoint.profiler.modifier.AbstractModifier;
import com.navercorp.pinpoint.profiler.modifier.arcus.interceptor.ArcusScope;
import com.navercorp.pinpoint.profiler.modifier.arcus.interceptor.FrontCacheGetFutureConstructInterceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.ProtectionDomain;

/**
 * @author harebox
 */
public class FrontCacheGetFutureModifier extends AbstractModifier {

    protected Logger logger;

    public FrontCacheGetFutureModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
        this.logger = LoggerFactory.getLogger(this.getClass());
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifying. {}", javassistClassName);
        }

        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);

            aClass.addTraceVariable("__cacheName", "__setCacheName", "__getCacheName", "java.lang.String");
            aClass.addTraceVariable("__cacheKey", "__setCacheKey", "__getCacheKey", "java.lang.String");

            SimpleAroundInterceptor frontCacheGetFutureConstructInterceptor = (SimpleAroundInterceptor) byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.navercorp.pinpoint.profiler.modifier.arcus.interceptor.FrontCacheGetFutureConstructInterceptor");
            aClass.addConstructorInterceptor(new String[]{"net.sf.ehcache.Element"}, frontCacheGetFutureConstructInterceptor);

            SimpleAroundInterceptor frontCacheGetFutureGetInterceptor = (SimpleAroundInterceptor) byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.navercorp.pinpoint.profiler.modifier.arcus.interceptor.FrontCacheGetFutureGetInterceptor");
            aClass.addGroupInterceptor("get", new String[]{Long.TYPE.toString(), "java.util.concurrent.TimeUnit"}, frontCacheGetFutureGetInterceptor, ArcusScope.SCOPE);
            
            SimpleAroundInterceptor frontCacheGetFutureGetInterceptor2 = (SimpleAroundInterceptor) byteCodeInstrumentor.newInterceptor(classLoader, protectedDomain, "com.navercorp.pinpoint.profiler.modifier.arcus.interceptor.FrontCacheGetFutureGetInterceptor");
            aClass.addGroupInterceptor("get", new String[]{}, frontCacheGetFutureGetInterceptor2, ArcusScope.SCOPE);

            return aClass.toBytecode();
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn(e.getMessage(), e);
            }
            return null;
        }
    }

    @Override
    public String getTargetClass() {
        return "net/spy/memcached/plugin/FrontCacheGetFuture";
    }
}
