/*
 * Copyright 2022-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.kinesis;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The base contract for JUnit tests based on the container for Localstack.
 * The Testcontainers 'reuse' option must be disabled,so, Ryuk container is started
 * and will clean all the containers up from this test suite after JVM exit.
 * Since the Localstack container instance is shared via static property, it is going to be
 * started only once per JVM, therefore the target Docker container is reused automatically.
 *
 * @author Artem Bilan
 *
 * @since 3.0
 */
@Testcontainers(disabledWithoutDocker = true)
public interface LocalstackContainerTest {

	/**
	 * The shared {@link LocalStackContainer} instance.
	 */
	LocalStackContainer LOCAL_STACK_CONTAINER =
			new LocalStackContainer(DockerImageName.parse("localstack/localstack:2.2.0"))
					.withEnv(Optional.ofNullable(System.getenv("GITHUB_API_TOKEN"))
							.map(value -> Map.of("GITHUB_API_TOKEN", value))
							.orElse(Map.of()));
	@BeforeAll
	static void startContainer() {
		LOCAL_STACK_CONTAINER.start();
	}

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.aws.endpoint", LOCAL_STACK_CONTAINER::getEndpoint);
		registry.add("spring.cloud.aws.region.static", LOCAL_STACK_CONTAINER::getRegion);
		registry.add("spring.cloud.aws.credentials.access-key", LOCAL_STACK_CONTAINER::getAccessKey);
		registry.add("spring.cloud.aws.credentials.secret-key", LOCAL_STACK_CONTAINER::getSecretKey);
	}

	static DynamoDbAsyncClient dynamoDbClient() {
		return applyAwsClientOptions(DynamoDbAsyncClient.builder());
	}

	static KinesisAsyncClient kinesisClient() {
		return applyAwsClientOptions(KinesisAsyncClient.builder());
	}

	static CloudWatchAsyncClient cloudWatchClient() {
		return applyAwsClientOptions(CloudWatchAsyncClient.builder());
	}

	static AwsCredentialsProvider credentialsProvider() {
		return StaticCredentialsProvider.create(
				AwsBasicCredentials.create(LOCAL_STACK_CONTAINER.getAccessKey(), LOCAL_STACK_CONTAINER.getSecretKey()));
	}

	private static <B extends AwsClientBuilder<B, T>, T> T applyAwsClientOptions(B clientBuilder) {
		return clientBuilder
				.region(Region.of(LOCAL_STACK_CONTAINER.getRegion()))
				.credentialsProvider(credentialsProvider())
				.endpointOverride(LOCAL_STACK_CONTAINER.getEndpoint())
				.build();
	}

}
