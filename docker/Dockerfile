FROM snap

# Create shared dirs
RUN mkdir /mnt/satimg
RUN mkdir -p /mnt/monitor-radar

# Install monitor-radar-core
RUN mkdir -p /opt/monitor-radar-core/libs
COPY target/dependency /opt/monitor-radar-core/libs
COPY target/monitor-radar-core-3.0.jar /opt/monitor-radar-core/libs