/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.ui.modeler.servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@ComponentScan(value = { "org.flowable.ui.modeler.rest.api", "org.flowable.ui.common.rest.exception" })
@EnableAsync
public class ApiDispatcherServletConfiguration extends WebMvcConfigurationSupport {

    @Autowired
    protected ObjectMapper objectMapper;

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        RequestMappingHandlerMapping requestMappingHandlerMapping = new RequestMappingHandlerMapping();
        requestMappingHandlerMapping.setRemoveSemicolonContent(false);
        return requestMappingHandlerMapping;
    }

    // Flowable REST APIs use Jackson 2 types (ObjectNode, ...); Spring 7 prefers Jackson 3 by default
    @Override
    protected void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.withJsonConverter(new MappingJackson2HttpMessageConverter(objectMapper));
    }
}
