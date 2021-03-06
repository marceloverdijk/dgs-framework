/*
 * Copyright 2021 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.graphql.dgs.internal

import com.netflix.graphql.dgs.DgsContextBuilder
import com.netflix.graphql.dgs.context.DgsContext
import com.netflix.graphql.dgs.context.DgsCustomContextBuilder
import com.netflix.graphql.dgs.context.DgsCustomContextBuilderWithRequest
import com.netflix.graphql.dgs.internal.utils.TimeTracer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.context.request.WebRequest
import java.util.*

open class DefaultDgsGraphQLContextBuilder(
    private val dgsCustomContextBuilder: Optional<DgsCustomContextBuilder<*>>,
    private val dgsCustomContextBuilderWithRequest: Optional<DgsCustomContextBuilderWithRequest<*>> = Optional.empty()
) : DgsContextBuilder {
    val logger: Logger = LoggerFactory.getLogger(DefaultDgsGraphQLContextBuilder::class.java)

    override fun build(): DgsContext {
        return TimeTracer.logTime({ buildDgsContext(null) }, logger, "Created DGS context in {}ms")
    }

    override fun build(dgsRequestData: DgsRequestData): DgsContext {
        return TimeTracer.logTime({ buildDgsContext(dgsRequestData) }, logger, "Created DGS context in {}ms")
    }

    private fun buildDgsContext(dgsRequestData: DgsRequestData?): DgsContext {
        @Suppress("DEPRECATION") val customContext = when {
            dgsCustomContextBuilderWithRequest.isPresent -> dgsCustomContextBuilderWithRequest.get().build(
                dgsRequestData?.extensions ?: mapOf(),
                HttpHeaders.readOnlyHttpHeaders(
                    dgsRequestData?.headers
                        ?: HttpHeaders()
                ),
                dgsRequestData?.webRequest
            )
            dgsCustomContextBuilder.isPresent -> dgsCustomContextBuilder.get().build()
            else
            // This is for backwards compatibility - we previously made DefaultRequestData the custom context if no custom context was provided.
            -> dgsRequestData
        }

        return DgsContext(
            customContext,
            dgsRequestData
        )
    }
}

@Deprecated("Use DgsContext.requestData instead")
data class DefaultRequestData(
    @Deprecated("Use DgsContext.requestData instead") val extensions: Map<String, Any>,
    @Deprecated("Use DgsContext.requestData instead") val headers: HttpHeaders
)

data class DgsRequestData(
    val extensions: Map<String, Any>? = emptyMap(),
    val headers: HttpHeaders? = HttpHeaders.readOnlyHttpHeaders(HttpHeaders()),
    val webRequest: WebRequest? = null
)
