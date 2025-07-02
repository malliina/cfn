echo "Building function..."
sbt "project simple" assembly
unzip simple/target/simple.jar -d "simple/target/jar"
echo "Running synth"
npm install -g aws-cdk@"2.1020.0"
cdk synth
