pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    tools {
        maven 'Maven3'   // Manage Jenkins > Tools: name must match
        jdk   'JDK17'
    }

    environment {
        DOCKERHUB_USER = 'YOUR_DOCKERHUB_USER'          // not a secret
        IMAGE_NAME     = "${DOCKERHUB_USER}/finsecure-pay"
        IMAGE_TAG      = "1.0.${BUILD_NUMBER}"
        K8S_NAMESPACE  = 'finsecure'
        // Credentials are stored in Jenkins (Manage Jenkins > Credentials), never in this file
        DOCKER_CREDS   = credentials('dockerhub-creds')  // username/password (access token)
        KUBECONFIG_CRED = 'kubeconfig-file'              // "Secret file" credential
        NVD_API_KEY    = credentials('nvd-api-key')      // "Secret text" credential
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build') {
            steps { sh 'mvn -B clean compile' }
        }

        stage('Test') {
            steps { sh 'mvn -B test' }
            post {
                always { junit 'target/surefire-reports/*.xml' }
            }
        }

        stage('Package') {
            steps { sh 'mvn -B package -DskipTests' }
        }

        stage('Code Quality (SonarCloud)') {
            steps {
                withSonarQubeEnv('SonarCloud') {          // Manage Jenkins > System > SonarQube servers
                    sh 'mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Dependency Scan (OWASP)') {
            steps {
                // Fails build on CVSS >= 7 (configured in pom.xml)
                sh 'mvn -B org.owasp:dependency-check-maven:check -DnvdApiKey=$NVD_API_KEY'
            }
            post {
                always { archiveArtifacts artifacts: 'target/dependency-check-report.*', allowEmptyArchive: true }
            }
        }

        stage('Container Build') {
            steps {
                sh 'docker build -t $IMAGE_NAME:$IMAGE_TAG -t $IMAGE_NAME:latest .'
            }
        }

        stage('Image Scan (Trivy)') {
            steps {
                // Gate: exit code 1 on HIGH/CRITICAL findings => pipeline fails, nothing is pushed
                sh '''
                  trivy image --severity HIGH,CRITICAL --exit-code 1 --ignore-unfixed \
                      --format table -o trivy-report.txt $IMAGE_NAME:$IMAGE_TAG
                '''
            }
            post {
                always { archiveArtifacts artifacts: 'trivy-report.txt', allowEmptyArchive: true }
            }
        }

        stage('Push Image') {
            steps {
                sh '''
                  echo "$DOCKER_CREDS_PSW" | docker login -u "$DOCKER_CREDS_USR" --password-stdin
                  docker push $IMAGE_NAME:$IMAGE_TAG
                  docker push $IMAGE_NAME:latest
                '''
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG')]) {
                    sh '''
                      kubectl apply -f k8s/00-namespace.yaml
                      kubectl apply -f k8s/01-configmap.yaml
                      kubectl apply -f k8s/03-service.yaml
                      kubectl apply -f k8s/02-deployment.yaml
                      kubectl -n $K8S_NAMESPACE set image deployment/finsecure-pay \
                          finsecure-pay=$IMAGE_NAME:$IMAGE_TAG
                      kubectl -n $K8S_NAMESPACE annotate deployment/finsecure-pay \
                          kubernetes.io/change-cause="build $BUILD_NUMBER image $IMAGE_TAG" --overwrite
                    '''
                }
            }
        }

        stage('Verify Rollout') {
            steps {
                withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG')]) {
                    sh '''
                      if ! kubectl -n $K8S_NAMESPACE rollout status deployment/finsecure-pay --timeout=180s; then
                        echo "Rollout failed - rolling back automatically"
                        kubectl -n $K8S_NAMESPACE rollout undo deployment/finsecure-pay
                        exit 1
                      fi
                      kubectl -n $K8S_NAMESPACE get pods -o wide
                    '''
                }
            }
        }
    }

    post {
        success {
            emailext subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Build and deployment succeeded.\nImage: ${env.IMAGE_NAME}:${env.IMAGE_TAG}\n${env.BUILD_URL}",
                     to: 'YOUR_EMAIL@example.com'
        }
        failure {
            emailext subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Pipeline failed. Check the console output: ${env.BUILD_URL}console",
                     to: 'YOUR_EMAIL@example.com'
        }
        always {
            sh 'docker logout || true'
        }
    }
}
