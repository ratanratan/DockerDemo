```groovy
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        AWS_REGION   = 'us-east-1'
        AWS_ACCOUNT  = '509571504433'
        ECR_REPO     = 'hello-docker'
        ECR_REGISTRY = '509571504433.dkr.ecr.us-east-1.amazonaws.com'
        DEPLOY_HOST  = '3.80.105.96'
    }

    stages {
        stage('Checkout GitHub') {
            steps {
                checkout scm

                script {
                    env.IMAGE_TAG = bat(
                        script: '@echo off\r\ngit rev-parse --short=12 HEAD',
                        returnStdout: true
                    ).trim()

                    env.IMAGE_URI =
                        "${env.ECR_REGISTRY}/${env.ECR_REPO}:${env.IMAGE_TAG}"

                    echo "Docker image: ${env.IMAGE_URI}"
                }
            }
        }

        stage('Build and Test') {
            steps {
                bat '''
                    @echo off
                    mvn clean verify
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                bat '''
                    @echo off
                    docker build -t "%IMAGE_URI%" .
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Push Image to ECR') {
            steps {
                bat '''
                    @echo off
                    aws ecr get-login-password --region "%AWS_REGION%" | docker login --username AWS --password-stdin "%ECR_REGISTRY%"
                    if errorlevel 1 exit /b 1

                    docker push "%IMAGE_URI%"
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Deploy to EC2') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'ec2-deploy-key',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        $remoteScript = @'
set -eu

echo "Logging in to Amazon ECR..."
aws ecr get-login-password --region "$AWS_REGION" |
    sudo docker login --username AWS --password-stdin "$ECR_REGISTRY"

echo "Pulling image: $IMAGE_URI"
sudo docker pull "$IMAGE_URI"

echo "Starting application..."
sudo docker rm -f hello-docker 2>/dev/null || true

sudo docker run -d `
    --name hello-docker `
    --restart unless-stopped `
    -p 80:9999 `
    "$IMAGE_URI"

sleep 10

if ! curl --fail --silent http://localhost/ >/dev/null; then
    sudo docker logs hello-docker || true
    exit 1
fi

echo "Deployment and HTTP health check succeeded."
'@

                        $remoteCommand = "AWS_REGION='$env:AWS_REGION' ECR_REGISTRY='$env:ECR_REGISTRY' IMAGE_URI='$env:IMAGE_URI' bash -s"

                        $remoteScript | & ssh.exe `
                            -i "$env:SSH_KEY" `
                            -o StrictHostKeyChecking=accept-new `
                            "$($env:SSH_USER)@$($env:DEPLOY_HOST)" `
                            $remoteCommand

                        if ($LASTEXITCODE -ne 0) {
                            throw "EC2 deployment failed with exit code $LASTEXITCODE"
                        }
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
            echo 'Pipeline failed. Check Console Output.'
        }
    }
}
```
