pipeline {
    agent any

    tools {
        jdk   'JDK-21'    // nombre del JDK registrado en Jenkins Global Tool Config
        maven 'Maven-3'   // nombre del Maven registrado en Jenkins Global Tool Config
    }

    environment {
        SONAR_PROJECT_KEY  = 'user-service'
        SONAR_PROJECT_NAME = 'User Service'

        // Credencial tipo "Username with password" creada en Jenkins Credentials Store
        DOCKERHUB_CREDENTIALS = credentials('dockerhub-credentials')
        DOCKERHUB_REPO        = "${DOCKERHUB_CREDENTIALS_USR}/user-service"
    }

    options {
        skipDefaultCheckout(true)
        timeout(time: 30, unit: 'MINUTES')
    }

    stages {

        // ---------------------------------------------------------------
        stage('Checkout') {
            steps {
                deleteDir()
                sh 'git -c http.sslVerify=false clone https://github.com/dozmaz/devsecops-microservices-java.git .'
            }
        }

        // ---------------------------------------------------------------
        // STAGE 1: Build — genera el FAT JAR usando spring-boot-maven-plugin
        // -pl user-service -am  →  compila sólo el módulo y sus dependientes
        // -DskipTests           →  evita reejecutar tests durante el empaquetado
        // ---------------------------------------------------------------
        stage('Build') {
            steps {
                echo '=== STAGE: Build — FAT JAR ==='
                sh 'mvn -pl user-service -am clean package -DskipTests --no-transfer-progress'
            }
            post {
                failure { error 'Build fallido. Revisa los errores de compilacion.' }
            }
        }

        // ---------------------------------------------------------------
        // STAGE 2: Testing — JUnit + JaCoCo
        // ---------------------------------------------------------------
        stage('Testing') {
            steps {
                echo '=== STAGE: Testing (JUnit + JaCoCo) ==='
                sh 'mvn -pl user-service -am test --no-transfer-progress'
            }
            post {
                always {
                    junit 'user-service/target/surefire-reports/*.xml'
                    jacoco(
                        execPattern:         'user-service/target/jacoco.exec',
                        classPattern:        'user-service/target/classes',
                        sourcePattern:       'user-service/src/main/java',
                        exclusionPattern:    '**/test/**',
                        minimumLineCoverage: '50'
                    )
                }
                failure { error 'Tests fallidos. Revisa los reportes JUnit/JaCoCo.' }
            }
        }

        // ---------------------------------------------------------------
        // STAGE 3: SonarQube
        // ---------------------------------------------------------------
        stage('Sonar') {
            steps {
                echo '=== STAGE: SonarQube Analysis ==='
                withSonarQubeEnv('SonarQube') {
                    sh """
                        mvn -pl user-service -am org.sonarsource.scanner.maven:sonar-maven-plugin:sonar --no-transfer-progress \\
                          -Dsonar.projectKey=${SONAR_PROJECT_KEY} \\
                          -Dsonar.projectName='${SONAR_PROJECT_NAME}' \\
                          -Dsonar.java.source=17 \\
                          -Dsonar.coverage.jacoco.xmlReportPaths=user-service/target/site/jacoco/jacoco.xml
                    """
                }
            }
            post {
                success {
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: true
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // STAGE 4: Deploy a Docker Hub
        // Construye imagen con el Dockerfile de user-service/ y hace push
        // ---------------------------------------------------------------
        stage('Deploy a Docker Hub') {
            steps {
                echo '=== STAGE: Imagen Docker -> Docker Hub ==='
                sh '''
                    docker build \
                        -t ${DOCKERHUB_REPO}:${BUILD_NUMBER} \
                        -t ${DOCKERHUB_REPO}:latest \
                        user-service/

                    echo ${DOCKERHUB_CREDENTIALS_PSW} | \
                        docker login -u ${DOCKERHUB_CREDENTIALS_USR} --password-stdin

                    docker push ${DOCKERHUB_REPO}:${BUILD_NUMBER}
                    docker push ${DOCKERHUB_REPO}:latest

                    docker logout
                '''
            }
            post {
                failure { error 'Push a Docker Hub fallido. Revisa las credenciales.' }
            }
        }
    }

    post {
        success { echo "Pipeline OK. Imagen: ${DOCKERHUB_REPO}:${BUILD_NUMBER}" }
        failure { echo "Pipeline FALLIDO en build #${BUILD_NUMBER}" }
        always  { cleanWs() }
    }
}
