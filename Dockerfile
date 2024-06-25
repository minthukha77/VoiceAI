FROM --platform=linux/amd64 centos:7
USER root
RUN chmod -R 777 /tmp/
RUN yum update -y
RUN yum install sudo -y
RUN yum install -y wget

RUN mkdir /usr/local/tomcat
RUN wget -c https://dlcdn.apache.org/tomcat/tomcat-9/v9.0.71/bin/apache-tomcat-9.0.71.tar.gz -O /tmp/tomcat.tar.gz
RUN cd /tmp && tar xvfz tomcat.tar.gz
RUN cp -Rv /tmp/apache-tomcat-9.0.71/* /usr/local/tomcat/
EXPOSE 8080

RUN wget --no-check-certificate -c --header "Cookie: oraclelicense=accept-securebackup-cookie" https://download.oracle.com/java/18/archive/jdk-18.0.2_linux-x64_bin.rpm
RUN rpm -ivh jdk-18.0.2_linux-x64_bin.rpm
RUN rm jdk-18.0.2_linux-x64_bin.rpm
RUN yum clean all

RUN cd /
RUN cd /usr/local/bin
RUN mkdir ffmpeg
RUN cd ffmpeg
RUN wget https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz
RUN tar -v -xf ffmpeg-release-amd64-static.tar.xz --strip-components=1
RUN rm ffmpeg-release-amd64-static.tar.xz
RUN yum clean all
RUN cp ffmpeg usr/bin/
RUN cd /

RUN mkdir -p -v /usr/local/src/static/audio_files
RUN mkdir -p -v /usr/local/src/static/wav_files
RUN mkdir -p -v /usr/local/src/static/logs
RUN chmod -R 777 /usr/local/src/static/audio_files
RUN chmod -R 777 /usr/local/src/static/wav_files
RUN chmod -R 777 /usr/local/src/static/logs
RUN cd /

RUN yum install -y \
                    curl \
                    which && \
    yum clean all
RUN curl -sSL https://sdk.cloud.google.com | bash
ENV PATH $PATH:/root/google-cloud-sdk/bin
COPY /src/static/key.json /usr/local/src/static/key.json
#aws credentials copy
COPY /src/static/config /home/ec2-user/.aws/config
COPY /src/static/credentials /home/ec2-user/.aws/credentials
RUN export GOOGLE_APPLICATION_CREDENTIALS=/usr/local/src/static/key.json
ENV GOOGLE_APPLICATION_CREDENTIALS /usr/local/src/static/key.json
#setting env variables
RUN export AWS_ACCESS_KEY_ID=/home/ec2-user/.aws/credentials
RUN export AWS_SECRET_ACCESS_KEY=/home/ec2-user/.aws/credentials
ENV AWS_ACCESS_KEY_ID "AKIARNJZF7G3B4SRLXNR"
ENV AWS_SECRET_ACCESS_KEY "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"
RUN gcloud auth activate-service-account voitra@voitra.iam.gserviceaccount.com --key-file=/usr/local/src/static/key.json
RUN gcloud config set account voitra.jp@gmail.com
RUN yum clean all

COPY target/SpringBootExecutableJar-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/

RUN mkdir -p /var/log/supervisor
RUN yum install python3-pip -y
RUN pip3 install supervisor

#install awscli
ENV AWS_DEFAULT_REGION='ap-northeast-1'
ENV AWS_ACCESS_KEY_ID='AKIARNJZF7G3B4SRLXNR'
ENV AWS_SECRET_ACCESS_KEY='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'
RUN pip3 install awscli

#install docker
RUN pip3 install docker

#RUN sudo  /etc/init.d/td-agent start
RUN yum clean all

COPY service_script.conf /etc/supervisor/conf.d/supervisord.conf
CMD ["supervisord","-c","/etc/supervisor/conf.d/supervisord.conf"]
#ENTRYPOINT [ "sh", "-c", "java -jar /usr/local/tomcat/webapps/SpringBootExecutableJar-0.0.1-SNAPSHOT.jar" ]