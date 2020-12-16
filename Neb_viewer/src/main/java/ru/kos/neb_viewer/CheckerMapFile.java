package ru.kos.neb_viewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import static ru.kos.neb_viewer.Main.history_list;
import static ru.kos.neb_viewer.Main.history_path;
import static ru.kos.neb_viewer.Main.utils;

public class CheckerMapFile extends Thread {
    public static long time_start = 0;
    
    @Override
    public void run() {
        time_start = System.currentTimeMillis();
        String prev_map_filename=history_list.get(TimeMachineForm.selector)[1];
        System.out.println("time_start="+time_start);
        while(true) {
            if(!Main.isBusy) {
                try {
                    BufferedReader inFile = new BufferedReader(new FileReader(Main.dump));
                    try {
                        String line1 = inFile.readLine().toString();
                        long time_from_file = Long.valueOf(line1);
                        if(time_from_file > time_start) {
                            time_start = System.currentTimeMillis();
                            System.out.println("time_start="+time_start);
                            history_list = utils.GetListMapFiles();
                            for(int i=0; i<history_list.size(); i++) {
                                String[] item = history_list.get(i);
                                if(prev_map_filename.equals(item[1])) {
                                    TimeMachineForm.selector=i;
                                    break;
                                }
                            }
                            Utils.SetTimeMachine(history_list, TimeMachineForm.selector, ControlPanel.area_select);
                        }
                    } catch (IOException ex) { }
                } catch (FileNotFoundException ex) { }
            
            }
            prev_map_filename=history_list.get(TimeMachineForm.selector)[1];
            try { Thread.sleep(10000); } catch (InterruptedException e) { }

        }
    }
}
