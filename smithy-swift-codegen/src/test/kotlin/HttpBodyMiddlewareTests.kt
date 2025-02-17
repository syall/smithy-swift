/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

import io.kotest.matchers.string.shouldContainOnlyOnce
import org.junit.jupiter.api.Test
import software.amazon.smithy.swift.codegen.model.AddOperationShapes

class HttpBodyMiddlewareTests {
    private var model = javaClass.getResource("http-binding-protocol-generator-test.smithy").asSmithy()
    var newTestContext: TestContext
    init {
        newTestContext = newTestContext()
        newTestContext.generator.generateSerializers(newTestContext.generationCtx)
        newTestContext.generator.generateProtocolClient(newTestContext.generationCtx)
        newTestContext.generator.generateDeserializers(newTestContext.generationCtx)
        newTestContext.generator.generateCodableConformanceForNestedTypes(newTestContext.generationCtx)
        newTestContext.generationCtx.delegator.flushWriters()
    }
    private fun newTestContext(): TestContext {
        val settings = model.defaultSettings()
        model = AddOperationShapes.execute(model, settings.getService(model), settings.moduleName)
        return model.newTestContext()
    }

    @Test
    fun `it builds body middleware for explicit string payloads`() {
        val contents = getModelFileContents("example", "ExplicitStringInput+BodyMiddleware.swift", newTestContext.manifest)
        contents.shouldSyntacticSanityCheck()
        val expectedContents =
            """
            public struct ExplicitStringInputBodyMiddleware: ClientRuntime.Middleware {
                public let id: Swift.String = "ExplicitStringInputBodyMiddleware"
            
                public init() {}
            
                public func handle<H>(context: Context,
                              input: ClientRuntime.SerializeStepInput<ExplicitStringInput>,
                              next: H) async throws -> ClientRuntime.OperationOutput<ExplicitStringOutput>
                where H: Handler,
                Self.MInput == H.Input,
                Self.MOutput == H.Output,
                Self.Context == H.Context
                {
                    if let payload1 = input.operationInput.payload1 {
                        let payload1Data = payload1.data(using: .utf8)
                        let payload1Body = ClientRuntime.HttpBody.data(payload1Data)
                        input.builder.withBody(payload1Body)
                    }
                    return try await next.handle(context: context, input: input)
                }
            
                public typealias MInput = ClientRuntime.SerializeStepInput<ExplicitStringInput>
                public typealias MOutput = ClientRuntime.OperationOutput<ExplicitStringOutput>
                public typealias Context = ClientRuntime.HttpContext
            }
            """.trimIndent()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `it builds body middleware for explicit blob payloads`() {
        val contents = getModelFileContents("example", "ExplicitBlobInput+BodyMiddleware.swift", newTestContext.manifest)
        contents.shouldSyntacticSanityCheck()
        val expectedContents =
            """
            public struct ExplicitBlobInputBodyMiddleware: ClientRuntime.Middleware {
                public let id: Swift.String = "ExplicitBlobInputBodyMiddleware"
            
                public init() {}
            
                public func handle<H>(context: Context,
                              input: ClientRuntime.SerializeStepInput<ExplicitBlobInput>,
                              next: H) async throws -> ClientRuntime.OperationOutput<ExplicitBlobOutput>
                where H: Handler,
                Self.MInput == H.Input,
                Self.MOutput == H.Output,
                Self.Context == H.Context
                {
                    if let payload1 = input.operationInput.payload1 {
                        let payload1Data = payload1
                        let payload1Body = ClientRuntime.HttpBody.data(payload1Data)
                        input.builder.withBody(payload1Body)
                    }
                    return try await next.handle(context: context, input: input)
                }
            
                public typealias MInput = ClientRuntime.SerializeStepInput<ExplicitBlobInput>
                public typealias MOutput = ClientRuntime.OperationOutput<ExplicitBlobOutput>
                public typealias Context = ClientRuntime.HttpContext
            }
            """.trimIndent()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `it builds body middleware for explicit streaming blob payloads`() {
        val contents = getModelFileContents("example", "ExplicitBlobStreamInput+BodyMiddleware.swift", newTestContext.manifest)
        contents.shouldSyntacticSanityCheck()
        val expectedContents =
            """
            public struct ExplicitBlobStreamInputBodyMiddleware: ClientRuntime.Middleware {
                public let id: Swift.String = "ExplicitBlobStreamInputBodyMiddleware"
            
                public init() {}
            
                public func handle<H>(context: Context,
                              input: ClientRuntime.SerializeStepInput<ExplicitBlobStreamInput>,
                              next: H) async throws -> ClientRuntime.OperationOutput<ExplicitBlobStreamOutput>
                where H: Handler,
                Self.MInput == H.Input,
                Self.MOutput == H.Output,
                Self.Context == H.Context
                {
                    if let payload1 = input.operationInput.payload1 {
                        let payload1Body = ClientRuntime.HttpBody(byteStream: payload1)
                        input.builder.withBody(payload1Body)
                    }
                    return try await next.handle(context: context, input: input)
                }
            
                public typealias MInput = ClientRuntime.SerializeStepInput<ExplicitBlobStreamInput>
                public typealias MOutput = ClientRuntime.OperationOutput<ExplicitBlobStreamOutput>
                public typealias Context = ClientRuntime.HttpContext
            }
            """.trimIndent()
        contents.shouldContainOnlyOnce(expectedContents)
    }

    @Test
    fun `it builds body middleware for explicit struct payloads`() {
        val contents = getModelFileContents("example", "ExplicitStructInput+BodyMiddleware.swift", newTestContext.manifest)
        contents.shouldSyntacticSanityCheck()
        val expectedContents =
            """
            public struct ExplicitStructInputBodyMiddleware: ClientRuntime.Middleware {
                public let id: Swift.String = "ExplicitStructInputBodyMiddleware"
            
                public init() {}
            
                public func handle<H>(context: Context,
                              input: ClientRuntime.SerializeStepInput<ExplicitStructInput>,
                              next: H) async throws -> ClientRuntime.OperationOutput<ExplicitStructOutput>
                where H: Handler,
                Self.MInput == H.Input,
                Self.MOutput == H.Output,
                Self.Context == H.Context
                {
                    do {
                        let encoder = context.getEncoder()
                        if let payload1 = input.operationInput.payload1 {
                            let payload1Data = try encoder.encode(payload1)
                            let payload1Body = ClientRuntime.HttpBody.data(payload1Data)
                            input.builder.withBody(payload1Body)
                        } else {
                            if encoder is JSONEncoder {
                                // Encode an empty body as an empty structure in JSON
                                let payload1Data = "{}".data(using: .utf8)!
                                let payload1Body = ClientRuntime.HttpBody.data(payload1Data)
                                input.builder.withBody(payload1Body)
                            }
                        }
                    } catch let err {
                        throw ClientRuntime.ClientError.unknownError(err.localizedDescription)
                    }
                    return try await next.handle(context: context, input: input)
                }
            
                public typealias MInput = ClientRuntime.SerializeStepInput<ExplicitStructInput>
                public typealias MOutput = ClientRuntime.OperationOutput<ExplicitStructOutput>
                public typealias Context = ClientRuntime.HttpContext
            }
            """.trimIndent()
        contents.shouldContainOnlyOnce(expectedContents)
    }
}
