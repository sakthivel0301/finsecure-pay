pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        timeout(time: 90, unit: 'MINUTES')
    }

    environment {
        JAVA_HOME     = '/opt/homebrew/opt/openjdk@17'
        PATH          = "${JAVA_HOME}/bin:/opt/homebrew/bin:/usr/local/bin:/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/share/google-cloud-sdk/bin:${env.PATH}"
        IMAGE_NAME    = 'sakthivel0301/finsecure-pay'
        IMAGE_TAG     = "1.0.${BUILD_NUMBER}"
        K8S_NAMESPACE = 'finsecure'
        // Secrets come from Jenkins Credentials; Jenkins masks them in the logs
        NVD_API_KEY   = credentials('nvd-api-key')
        SONAR_TOKEN   = credentials('sonar-token')
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

        stage('Code Quality - SonarCloud') {
            steps {
                // Blocks the pipeline if the SonarCloud quality gate fails
                sh '''
                  mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
                      -Dsonar.token=$SONAR_TOKEN \
                      -Dsonar.qualitygate.wait=true
                '''
            }
        }

        stage('Dependency Scan - OWASP') {
            steps {
                // Fails the build on CVSS >= 7 (High/Critical), set in pom.xml
                sh 'mvn -B org.owasp:dependency-check-maven:check'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/dependency-check-report.*', allowEmptyArchive: true
                }
            }
        }

        stage('Container Build') {
            steps {
                sh 'docker buildx build --pull --platform linux/amd64 --load -t $IMAGE_NAME:$IMAGE_TAG .'
            }
        }

        stage('Image Scan - Trivy') {
            steps {
                sh '''
                  # Readable report first (never fails), then the gate
                  trivy image --severity HIGH,CRITICAL --ignore-unfixed --no-progress \
                      --exit-code 0 $IMAGE_NAME:$IMAGE_TAG | tee trivy-report.txt
                  trivy image --severity HIGH,CRITICAL --ignore-unfixed --no-progress \
                      --exit-code 1 --quiet $IMAGE_NAME:$IMAGE_TAG
                '''
            }
            post {
                always { archiveArtifacts artifacts: 'trivy-report.txt', allowEmptyArchive: true }
            }
        }

        stage('Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                  usernameVariable: 'DH_USER',
                                                  passwordVariable: 'DH_PASS')]) {
                    sh '''
                      echo "$DH_PASS" | docker login -u "$DH_USER" --password-stdin
                      docker push $IMAGE_NAME:$IMAGE_TAG
                      docker logout
                    '''
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh '''
                  kubectl apply -f k8s/00-namespace.yaml -f k8s/01-configmap.yaml -f k8s/03-service.yaml
                  sed "s#image: .*#image: $IMAGE_NAME:$IMAGE_TAG#" k8s/02-deployment.yaml | kubectl apply -f -
                  kubectl -n $K8S_NAMESPACE annotate deployment/finsecure-pay \
                      kubernetes.io/change-cause="build $BUILD_NUMBER image $IMAGE_TAG" --overwrite
                '''
            }
        }

        stage('Verify Rollout') {
            steps {
                sh '''
                  if ! kubectl -n $K8S_NAMESPACE rollout status deployment/finsecure-pay --timeout=240s; then
                    echo "Rollout failed - rolling back"
                    kubectl -n $K8S_NAMESPACE rollout undo deployment/finsecure-pay
                    exit 1
                  fi
                  kubectl -n $K8S_NAMESPACE get pods -o wide
                '''
            }
        }
    }

    post {
        success {
            emailext subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Pipeline succeeded.\nImage: ${env.IMAGE_NAME}:${env.IMAGE_TAG}\n${env.BUILD_URL}",
                     to: '$DEFAULT_RECIPIENTS'
        }
        failure {
            emailext subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
                     body: "Pipeline failed. See the red stage in the console: ${env.BUILD_URL}console",
                     to: '$DEFAULT_RECIPIENTS'
        }
    }
}
