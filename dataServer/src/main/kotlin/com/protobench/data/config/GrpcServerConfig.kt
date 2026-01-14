package com.protobench.data.config

import com.protobench.data.grpc.GrpcDataService
import io.grpc.Server
import io.grpc.ServerBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
class GrpcServerConfig(
    private val grpcDataService: GrpcDataService,
    @Value("\${grpc.server.port:9091}") private val grpcPort: Int
) {
    private lateinit var server: Server

    @PostConstruct
    fun start() {
        server = ServerBuilder
            .forPort(grpcPort)
            .addService(grpcDataService)
            .maxInboundMessageSize(10 * 1024 * 1024)  // 10MB
            .build()
            .start()

        println("✅ gRPC Server started on port $grpcPort")

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down gRPC server...")
            stop()
        })
    }

    @PreDestroy
    fun stop() {
        if (::server.isInitialized) {
            server.shutdown()
            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow()
                }
            } catch (e: InterruptedException) {
                server.shutdownNow()
            }
        }
    }
}