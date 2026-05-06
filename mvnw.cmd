@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    https://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@REM ----------------------------------------------------------------------------
@REM Apache Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------

@IF "%__MVNW_ARG0_NAME__%"=="" (SET "BASE_DIR=%~dp0") ELSE SET "BASE_DIR=%__MVNW_ARG0_NAME__%"

@SET MAVEN_PROJECTBASEDIR=%BASE_DIR%
@IF "%MAVEN_PROJECTBASEDIR%"=="" goto error

@IF NOT "%MVNW_USERNAME%"=="" (
  @SET MVNW_REPOURL=https://repo.maven.apache.org/maven2
)

@SET WRAPPER_DIR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper
@SET WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties
@SET WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar

@FOR /F "usebackq tokens=1,2 delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
    @IF "%%A"=="distributionUrl" SET MAVEN_DISTRIBUTION_URL=%%B
    @IF "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
)

@SET MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists
@SET MVN_CMD=mvn

@REM Try system Maven first
@WHERE mvn >nul 2>nul
@IF %ERRORLEVEL% EQU 0 (
    mvn %*
    GOTO end
)

@REM Fall back to downloading Maven via the wrapper
@SET "JAVA_HOME_MSG="
@IF "%JAVA_HOME%"=="" (
    @ECHO Warning: JAVA_HOME not set. Trying PATH...
)

@ECHO Downloading Maven from %MAVEN_DISTRIBUTION_URL%...
@ECHO.
@ECHO If this takes too long, please install Maven manually:
@ECHO   https://maven.apache.org/download.cgi
@ECHO   or run:  winget install Apache.Maven
@ECHO.

@powershell -Command "& { $url='%MAVEN_DISTRIBUTION_URL%'; $dest='%MAVEN_HOME%'; if (-not (Test-Path $dest)) { New-Item -ItemType Directory -Path $dest -Force | Out-Null }; Write-Host 'Please install Maven via: winget install Apache.Maven'; exit 1 }"
@IF %ERRORLEVEL% NEQ 0 (
    @ECHO.
    @ECHO ERROR: Maven is not installed. Please run:
    @ECHO   winget install Apache.Maven
    @ECHO Then re-run this command.
    @EXIT /B 1
)

:end
