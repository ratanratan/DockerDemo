
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        AWS_REGION   = 'us-east-1'
        AWS_ACCOUNT  = '509571504433'
        ECR_REPO     = 'hello-docker'
        ECR_REGISTRY = '509571504433.dkr.ecr.us-east-1.amazonaws.com'
        DEPLOY_HOST  = '3.80.105.96'
        APP_PORT     = '9999'
    }

    stages {
        stage('Checkout GitHub') {
            steps {
                checkout scm
                script {
                    env.IMAGE_TAG = sh(
                        script: 'git rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()
                    env.IMAGE_URI = "${env.ECR_REGISTRY}/${env.ECR_REPO}:${env.IMAGE_TAG}"
                }
            }
        }

        stage('Build and Test') {
            steps {
                sh '''
                    set -eu
                    mvn clean verify
                    test -f target/SpringBootHelloWorld-0.0.1-SNAPSHOT.jar
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    set -eu
                    docker build -t "$IMAGE_URI" .
                '''
            }
        }

        stage('Push Image to ECR') {
            steps {
                sh '''
                    set -eu

                    aws ecr get-login-password \
                      --region "$AWS_REGION" |
                    docker login \
                      --username AWS \
                      --password-stdin "$ECR_REGISTRY"

                    docker push "$IMAGE_URI"
                '''
            }
        }

        stage('Deploy to EC2') {
            steps {
                sshagent(credentials: ['ec2-deploy-key']) {
                    sh '''
                        set -eu

                        ssh -o StrictHostKeyChecking=accept-new \
                          ec2-user@"$DEPLOY_HOST" \
                          "AWS_REGION='$AWS_REGION' \
                           ECR_REGISTRY='$ECR_REGISTRY' \
                           IMAGE_URI='$IMAGE_URI' \
                           bash -s" <<'REMOTE_SCRIPT'
set -eu

aws ecr get-login-password --region "$AWS_REGION" |
  sudo docker login --username AWS --password-stdin "$ECR_REGISTRY"

sudo docker pull "$IMAGE_URI"

# Keep the current container available for a basic rollback.
sudo docker rm -f hello-docker-previous 2>/dev/null || true

if sudo docker container inspect hello-docker >/dev/null 2>&1; then
    sudo docker stop hello-docker
    sudo docker rename hello-docker hello-docker-previous
fi

if ! sudo docker run -d \
    --name hello-docker \
    --restart unless-stopped \
    -p 80:9999 \
    "$IMAGE_URI"; then

    sudo docker rm -f hello-docker 2>/dev/null || true

    if sudo docker container inspect hello-docker-previous >/dev/null 2>&1; then
        sudo docker rename hello-docker-previous hello-docker
        sudo docker start hello-docker
    fi

    exit 1
fi

# Give Spring Boot time to start.
sleep 10

if ! curl --fail --silent http://localhost/ >/dev/null; then
    sudo docker logs hello-docker || true
    sudo docker rm -f hello-docker

    if sudo docker container inspect hello-docker-previous >/dev/null 2>&1; then
        sudo docker rename hello-docker-previous hello-docker
        sudo docker start hello-docker
    fi

    exit 1
fi

sudo docker rm -f hello-docker-previous 2>/dev/null || true
echo "Deployment and HTTP health check succeeded."
REMOTE_SCRIPT
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "Deployment succeeded: ${env.IMAGE_URI}"
            echo "Application URL: http://${env.DEPLOY_HOST}/"
        }
        failure {
            echo 'Pipeline failed. Check Console Output and application logs.'
        }
    }
}
