FROM atmoz/sftp:alpine

COPY keys/keypair.pub /home/sftp/.ssh/keys/keypair.pub
COPY keys/ssh_host_ed25519_key /etc/ssh/ssh_host_ed25519_key
COPY keys/ssh_host_rsa_key /etc/ssh/ssh_host_rsa_key
RUN chmod 600 /home/sftp/.ssh/keys/keypair.pub
RUN chmod 600 /etc/ssh/ssh_host_ed25519_key
RUN chmod 600 /etc/ssh/ssh_host_rsa_key
HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD netstat -an | grep "0.0.0.0:22" -q
