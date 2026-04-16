@echo off
set DIR=%~dp0
set GRADLE_USER_HOME=%DIR%\.gradle
if not exist "%DIR%gradle\wrapper\gradle-wrapper.jar" (
  echo Gradle wrapper jar not found!
  exit /b 1
)
java -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
