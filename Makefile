artifact_name       := ocr-api
version             := "unversioned"

OS := $(shell uname)

.PHONY: all
all: build

.PHONY: clean
clean:
	mvn clean
	rm -f ./$(artifact_name).jar
	rm -f ./$(artifact_name)-*.zip
	rm -rf ./build-*
	rm -f ./build.log

.PHONY: build
build:
	mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
	mvn package -DskipTests=true
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar

.PHONY: test
test: clean
	mvn verify

.PHONY: test-unit
test-unit: clean
	mvn test -Dincluded.tests="unit-test"

.PHONY: test-integration
test-integration: clean
	mvn test -Dincluded.tests="integration-test"

.PHONY: dev
dev: clean
	mvn package
	cp target/$(artifact_name)-unversioned.jar $(artifact_name).jar

.PHONY: package
package:
ifndef version
	$(error No version given. Aborting)
endif
	$(info Packaging version: $(version) on $(OS))
ifneq ($(OS),Darwin)
		mvn versions:set -DnewVersion=$(version) -DgenerateBackupPoms=false
endif
	mvn package -DskipTests=true
	$(eval tmpdir:=$(shell mktemp -d build-XXXXXXXXXX))
	cp ./target/$(artifact_name)-$(version).jar $(tmpdir)/$(artifact_name).jar
ifeq ($(OS),Darwin)
	cp ./target/$(artifact_name)-$(version).jar ./$(artifact_name).jar
endif
	cd $(tmpdir); zip -r ../$(artifact_name)-$(version).zip *
	rm -rf $(tmpdir)

.PHONY: dist
dist: clean build package


.PHONY: sonar
sonar:
	mvn sonar:sonar

.PHONY: sonar-pr-analysis
sonar-pr-analysis:
	mvn sonar:sonar	-P sonar-pr-analysis

.PHONY: dependency-check
dependency-check:
	@ if [ -n "$(DEPENDENCY_CHECK_SUPPRESSIONS_HOME)" ]; then \
		if [ -d "$(DEPENDENCY_CHECK_SUPPRESSIONS_HOME)" ]; then \
			suppressions_home="$${DEPENDENCY_CHECK_SUPPRESSIONS_HOME}"; \
		else \
			printf -- 'DEPENDENCY_CHECK_SUPPRESSIONS_HOME is set, but its value "%s" does not point to a directory\n' "$(DEPENDENCY_CHECK_SUPPRESSIONS_HOME)"; \
			exit 1; \
		fi; \
	fi; \
	if [ ! -d "$${suppressions_home}" ]; then \
		suppressions_home_target_dir="./target/dependency-check-suppressions"; \
		if [ -d "$${suppressions_home_target_dir}" ]; then \
			suppressions_home="$${suppressions_home_target_dir}"; \
		else \
			mkdir -p "./target"; \
			git clone git@github.com:companieshouse/dependency-check-suppressions.git "$${suppressions_home_target_dir}" && \
				suppressions_home="$${suppressions_home_target_dir}"; \
		fi; \
	fi; \
	printf -- 'suppressions_home="%s"\n' "$${suppressions_home}"; \
	DEPENDENCY_CHECK_SUPPRESSIONS_HOME="$${suppressions_home}" "$${suppressions_home}/scripts/depcheck" --repo-name=ocr-api

.PHONY: security-check
security-check: dependency-check

