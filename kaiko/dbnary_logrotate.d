/var/log/dbnary/*.log {
        weekly
        missingok
        rotate 52
        compress
        delaycompress
        notifempty
        create 640 serasset adm
        sharedscripts
        postrotate
                if [ -f /var/run/apache.pid ]; then
                        /etc/init.d/apache restart > /dev/null
                fi
        endscript
}
