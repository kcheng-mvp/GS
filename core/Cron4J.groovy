#! /usr/bin/env groovy

@Grapes(
        @Grab(group='it.sauronsoftware.cron4j', module='cron4j', version='2.2.5')
)

import it.sauronsoftware.cron4j.Scheduler;



def start(cron, runnable){
    def s = new Scheduler();
    s.schedule(cron, runnable);
    s.start();
}

