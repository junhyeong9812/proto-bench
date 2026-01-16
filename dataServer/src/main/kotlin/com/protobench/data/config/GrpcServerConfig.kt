package com.protobench.data.config

import com.protobench.data.grpc.GrpcDataService
import io.grpc.Server
import io.grpc.ServerBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

/**
 * gRPC 서버 설정
 *
 * Spring Boot 시작 시 gRPC 서버를 별도 포트(기본 9091)에서 구동한다.
 * HTTP 서버(8081)와 독립적으로 동작한다.
 */
@Configuration
class GrpcServerConfig(
    private val grpcDataService: GrpcDataService,
    @Value("\${grpc.server.port:9091}") private val grpcPort: Int
) {
    private lateinit var server: Server

    /**
     * gRPC 서버 시작
     *
     * Spring 컨텍스트 초기화 후 자동 호출된다.
     */
    @PostConstruct
    fun start() {
        server = ServerBuilder
            .forPort(grpcPort)
            .addService(grpcDataService)
            .maxInboundMessageSize(10 * 1024 * 1024)
            .build()
            .start()

        println("✅ gRPC Server started on port $grpcPort")

        Runtime.getRuntime().addShutdownHook(Thread {
            println("Shutting down gRPC server...")
            stop()
        })
    }

    /**
     * gRPC 서버 종료
     *
     * Spring 컨텍스트 종료 시 자동 호출된다.
     */
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