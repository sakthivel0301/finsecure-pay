#!/usr/bin/env bash
# Task 5: controlled security findings. Run each on a branch, push, and capture the failed Jenkins build.
set -e
case "$1" in
  vuln-dependency)
    # Known-vulnerable dependency (Log4Shell, CVSS 10) -> OWASP Dependency-Check gate must FAIL the build
    python3 - <<'EOF'
import re
p=open('pom.xml').read()
dep="""    <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      <version>2.14.1</version>
    </dependency>
"""
if 'log4j-core' not in p:
    p=p.replace('<dependencies>','<dependencies>\n'+dep,1)
    open('pom.xml','w').write(p)
EOF
    echo "Added log4j-core 2.14.1. Commit + push to see the gate block the pipeline." ;;
  fix-dependency)
    # Remediation: remove the vulnerable dependency
    python3 - <<'EOF'
import re
p=open('pom.xml').read()
p=re.sub(r'\s*<dependency>\s*<groupId>org.apache.logging.log4j</groupId>.*?</dependency>\n','\n',p,flags=re.S)
open('pom.xml','w').write(p)
EOF
    echo "Removed vulnerable dependency. Commit + push to show a green build." ;;
  vuln-image)
    # Old base image with known HIGH/CRITICAL CVEs -> Trivy gate must FAIL
    sed -i 's#^FROM eclipse-temurin:17-jre-alpine#FROM eclipse-temurin:17.0.2_8-jre-focal#' Dockerfile
    echo "Switched to an old base image. Commit + push to see Trivy block the release." ;;
  fix-image)
    sed -i 's#^FROM eclipse-temurin:17.0.2_8-jre-focal#FROM eclipse-temurin:17-jre-alpine#' Dockerfile
    echo "Restored current base image. Commit + push for a green build." ;;
  failing-test)
    # Task 4: controlled failing unit test
    cat > src/test/java/com/finsecure/pay/ControlledFailureTest.java <<'EOF'
package com.finsecure.pay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
class ControlledFailureTest {
    @Test void controlledFailure() { fail("Deliberate failure to prove the pipeline blocks deployment"); }
}
EOF
    echo "Added failing test. Commit + push; Jenkins must stop at Test stage." ;;
  fix-test)
    rm -f src/test/java/com/finsecure/pay/ControlledFailureTest.java
    echo "Removed failing test. Commit + push for a green build." ;;
  *)
    echo "Usage: $0 {vuln-dependency|fix-dependency|vuln-image|fix-image|failing-test|fix-test}"; exit 1 ;;
esac
