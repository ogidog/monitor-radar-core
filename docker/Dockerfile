FROM snap

# Install monitor-radar-core
RUN mkdir -p /mnt/tasks
RUN mkdir -p /mnt/results
RUN mkdir -p /opt/monitor-radar-core/libs
COPY target/dependency /opt/monitor-radar-core/libs
COPY target/monitor-radar-core-3.0.jar /opt/monitor-radar-core/libs