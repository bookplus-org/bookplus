// ============================================================================
// BookPlus — Pipeline declarativo de Jenkins.
//
// Espeja el pipeline de GitHub Actions (CI/CD/Security) para entornos que usan Jenkins
// (habitual en banca/gran empresa). NO sustituye a GitHub Actions; es una alternativa
// equivalente, "pipeline-as-code", para demostrar portabilidad del CI.
//
// Requisitos en Jenkins:
//   - Plugins: Pipeline, Docker Pipeline, Git.
//   - El agente debe tener acceso a Docker (socket montado) — ver docker-compose.jenkins.yml.
//   - Credenciales: 'ghcr' (usuario/token de GHCR) y 'sonar-token' (token de SonarCloud).
// ============================================================================
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        REGISTRY     = 'ghcr.io/dhuarocc'
        MAVEN_IMAGE  = 'maven:3.9-eclipse-temurin-21'
        // Microservicios Maven a construir/testear/empaquetar.
        SERVICES     = 'book-plus-auth-service book-plus-catalog-service book-plus-cart-service book-plus-order-service book-plus-payment-service book-plus-inventory-service book-plus-notification-service book-plus-report-service book-plus-api-gateway'
    }

    stages {

        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build & Test') {
            agent { docker { image "${MAVEN_IMAGE}"; args '-v $HOME/.m2:/root/.m2' reuseNode true } }
            steps {
                sh '''
                    set -e
                    for svc in $SERVICES; do
                        echo "==== mvn test: $svc ===="
                        (cd "$svc" && mvn -B -q test)
                    done
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('SonarCloud') {
            when { branch 'main' }
            agent { docker { image "${MAVEN_IMAGE}"; args '-v $HOME/.m2:/root/.m2' reuseNode true } }
            environment { SONAR_TOKEN = credentials('sonar-token') }
            steps {
                sh '''
                    set -e
                    # Ejemplo sobre order-service (jacoco + sonar). Repetir por servicio según necesidad.
                    (cd book-plus-order-service && mvn -B -q verify sonar:sonar \
                        -Dsonar.host.url=https://sonarcloud.io -Dsonar.token=$SONAR_TOKEN)
                '''
            }
        }

        stage('Package') {
            agent { docker { image "${MAVEN_IMAGE}"; args '-v $HOME/.m2:/root/.m2' reuseNode true } }
            steps {
                sh '''
                    set -e
                    for svc in $SERVICES; do
                        (cd "$svc" && mvn -B -q -DskipTests package)
                    done
                '''
            }
        }

        stage('Docker build & push') {
            when { branch 'main' }
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr',
                        usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
                    sh '''
                        set -e
                        SHA=$(git rev-parse --short HEAD)
                        echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USER" --password-stdin
                        for svc in $SERVICES; do
                            IMG="$REGISTRY/$svc:$SHA"
                            docker build -t "$IMG" "$svc"
                            docker push "$IMG"
                        done
                    '''
                }
            }
        }

        stage('Trivy scan') {
            when { branch 'main' }
            steps {
                sh '''
                    set -e
                    SHA=$(git rev-parse --short HEAD)
                    for svc in $SERVICES; do
                        docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                            aquasec/trivy:latest image --severity HIGH,CRITICAL --exit-code 0 \
                            "$REGISTRY/$svc:$SHA"
                    done
                '''
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            steps {
                input message: '¿Desplegar a producción?', ok: 'Desplegar'
                echo 'Aquí iría el despliegue (docker compose up / kubectl apply).'
            }
        }
    }

    post {
        success { echo '✅ Pipeline OK' }
        failure { echo '❌ Pipeline FALLÓ' }
    }
}
