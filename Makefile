name_pkg="cc.jchu.app.mockdns"
name_launch_activity="cc.jchu.app.mockdns.MainActivity"

ARGS=--no-scan -PtargetSdkVersion=29

## all:	Build, install, and Run
.PHONY: all
all: dev
	@echo "Done"

.PHONY: lint
lint:
	./gradlew $(ARGS) :ktlint

## test-class:	test specific class by $ make test-class CLASS=com.linecorp.liff.YourTest
.PHONY: test-class
test-class:
	./gradlew $(ARGS) :liff:testDebugUnitTest --tests $(CLASS)

## test-app:	Run unit tests for app module only
.PHONY: test-app
test-app:
	./gradlew $(ARGS) :app:testDevelopDebugUnitTest

## test-liff:	Run unit tests for liff module only
.PHONY: test-liff
test-liff:
	./gradlew $(ARGS) :liff:testDebugUnitTest

## test:	Run unit tests
.PHONY: test
test: test-app test-liff

## clean:	remove generated files
.PHONY: clean
clean:
	./gradlew $(ARGS) clean

# use dev variant by default #

.PHONY: install
install: dev-install

.PHONY: launch
launch: dev-launch

# dev build #
.PHONY: dev
dev: dev-install dev-launch

.PHONY: dev-build
dev-build:
	./gradlew $(ARGS) :app:assembleDebug

.PHONY: dev-install
dev-install:
	./gradlew $(ARGS) installDebug

.PHONY: dev-launch
dev-launch:
	adb shell am start -n $(name_pkg)/$(name_launch_activity)


# commands for debugging #
.PHONY: debug
debug:
	adb shell am set-debug-app -w $(name_pkg).dev

.PHONY: nodebug
nodebug:
	adb shell am clear-debug-app
	@echo "clear debug"

help:
	@fgrep -h "##" $(MAKEFILE_LIST) | fgrep -v fgrep | sed -e 's/\\$$//' | sed -e 's/##//'

