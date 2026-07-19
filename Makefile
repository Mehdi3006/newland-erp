.DEFAULT_GOAL := help

.PHONY: help bootstrap check format test architecture sbom clean

help:
	@echo "bootstrap     Install locked JavaScript dependencies"
	@echo "check         Run all repository quality gates"
	@echo "format        Apply JavaScript, documentation, and Gradle formatting"
	@echo "test          Run JVM and JavaScript tests"
	@echo "architecture  Verify Phase P1 architecture constraints"
	@echo "sbom          Generate JVM and repository SBOMs"
	@echo "clean         Remove generated build output"

bootstrap:
	corepack enable
	pnpm install --frozen-lockfile

check:
	pnpm check
	./gradlew check

format:
	pnpm format
	./gradlew spotlessApply

test:
	pnpm test
	./gradlew test

architecture:
	pnpm architecture:verify

sbom:
	./gradlew cyclonedxBom

clean:
	pnpm exec nx reset
	./gradlew clean
