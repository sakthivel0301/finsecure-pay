pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        JAVA_HOME     = '/opt/homebrew/opt/openjdk@17'
        PATH          = "${JAVA_HOME}/bin:/opt/homebrew/bin:/usr/local/bin:/Applications/Docker.app/Contents/Resources/bin:/opt/homebrew/share/google-cloud-sdk/bin:${env.PATH}"
        IMAGE_NAME    = 'sakthivel0301/finsecure-pay'
        IMAGE_TAG     = "1.0.${BUILD_NUMBER}"
        K8S_NAMESPACE = 'finsecure'
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

        // Task 5 will add: SonarCloud, OWASP Dependency-Check, Trivy (see Jenkinsfile.full)

        stage('Container Build') {
            steps {
                // amd64 because the GKE nodes are amd64 and this Mac is arm64
                sh 'docker buildx build --platform linux/amd64 --load -t $IMAGE_NAME:$IMAGE_TAG .'
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
        success { echo "Deployed $IMAGE_NAME:$IMAGE_TAG" }
        failure { echo "Pipeline failed - see the stage that turned red" }
    }
}
