/*
 * Copyright (C) 2019 University of South Florida
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.usf.cutr.tba;


import edu.usf.cutr.tba.exception.FirebaseFileNotInitializedException;
import edu.usf.cutr.tba.manager.TravelBehaviorDataAnalysisManager;
import edu.usf.cutr.tba.options.ProgramOptions;
import edu.usf.cutr.tba.utils.StringUtils;
import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcessorMain {
    public static void main(String[] args) {
        Options options = createCommandLineOptions();
        ProgramOptions programOptions = ProgramOptions.getInstance();

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption(ProgramOptions.KEY_FILE)) {
                programOptions.setKeyFilePath(cmd.getOptionValue(ProgramOptions.KEY_FILE));
            } else {
                System.err.println("Firebase admin key is not provided. \n" +
                        "Provide an admin key using -keyFile path/to/file.json");
                return;
            }

            if (cmd.hasOption(ProgramOptions.USER_ID)) {
                programOptions.setUserId(cmd.getOptionValue(ProgramOptions.USER_ID));
            }

            if (cmd.hasOption(ProgramOptions.NO_MERGE_STILL)) {
                programOptions.setMergeStillEventsEnabled(false);
            }

            if (cmd.hasOption(ProgramOptions.NO_MERGE_WALKING_RUNNING)) {
                programOptions.setMergeAllWalkingAndRunningEventsEnabled(false);
            }

            if (cmd.hasOption(ProgramOptions.SAME_DAY_START_POINT)) {
                String value = cmd.getOptionValue(ProgramOptions.SAME_DAY_START_POINT);
                Integer i = Integer.valueOf(value);
                programOptions.setSameDayStartPoint(i);
            }

            if (cmd.hasOption(ProgramOptions.STILL_EVENT_MERGE_THRESHOLD)) {
                String value = cmd.getOptionValue(ProgramOptions.STILL_EVENT_MERGE_THRESHOLD);
                Integer i = Integer.valueOf(value);
                programOptions.setStillEventMergeThreshold(i);
            }

            if (cmd.hasOption(ProgramOptions.WALKING_RUNNING_EVENT_MERGE_THRESHOLD)) {
                String value = cmd.getOptionValue(ProgramOptions.WALKING_RUNNING_EVENT_MERGE_THRESHOLD);
                Integer i = Integer.valueOf(value);
                programOptions.setWalkingRunningEventMergeThreshold(i);
            }

            if (cmd.hasOption(ProgramOptions.START_DATE) && cmd.hasOption(ProgramOptions.END_DATE)) {
                long dateStartMillis = StringUtils.validateStringDateAndParseToMillis(cmd.getOptionValue(ProgramOptions.START_DATE));
                long dateEndMillis = StringUtils.validateStringDateAndParseToMillis(cmd.getOptionValue(ProgramOptions.END_DATE));

                //Validate dates
                if (dateStartMillis == 0 || dateEndMillis == 0) {
                    System.err.println("Invalid start/end dates provided. \n" +
                            "Please provide dates in using the format mm-dd-yyyy.");
                    return;
                }
                programOptions.setStartDate(dateStartMillis);
                programOptions.setEndDate(dateEndMillis);

            } else if (cmd.hasOption(ProgramOptions.START_DATE)) {
                System.err.println("startDate and endDate must be provided together. \n" +
                        "startDate was provided but endDate was not provided.");
                return;
            } else if (cmd.hasOption(ProgramOptions.END_DATE)) {
                System.err.println("startDate and endDate must be provided together. \n" +
                        "endDate was provided but startDate was not provided.");
                return;
            }

            // Verify and process custom output folder
            if (cmd.hasOption(ProgramOptions.SAVE_ON_PATH)) {
                String argDir = cmd.getOptionValue(ProgramOptions.SAVE_ON_PATH);
                // Validate if the argDir is a valid path, if not exists then try to create it
                String newDir = StringUtils.validateAndParseOutputPath(argDir);

                if (!newDir.isEmpty()) {
                    programOptions.setOutputDir(newDir);
                } else {
                    // Error messages were provided in the validateAndParseOutputPath() method.
                    return;
                }
            }

            // Verify noKMZ option
            if (cmd.hasOption(ProgramOptions.SKIP_KMZ)) {
                programOptions.setSkipKmz(true);
            }

            // Verify and process file with multiple users
            if (cmd.hasOption(ProgramOptions.MULTI_USERS_PATH)) {
                String argMultiUserPath = cmd.getOptionValue(ProgramOptions.MULTI_USERS_PATH);
                // Validate if the argMultiUserPath exists, if not exists then return
                try {
                    Path localPath = Paths.get(argMultiUserPath);
                    if (!Files.exists(localPath)) {
                        System.err.println("The provided csv file for multiple userId's does not exist.");
                        return;
                    }
                    programOptions.setMultiUserId(localPath.toString());
                } catch (InvalidPathException e) {
                    System.err.println("Invalid command line option. multiUserId is not a valid path." + e);
                    return;
                }
            }

        } catch (ParseException e) {
            System.err.println("Invalid command line options");
        }

        System.out.println("Analysis started!");
        try {
            new TravelBehaviorDataAnalysisManager().processData();
        } catch (FirebaseFileNotInitializedException e) {
            System.err.println("Firebase file is not initialized properly.");
        }

        System.out.println("Analysis finished!");
    }

    private static Options createCommandLineOptions() {
        Options options = new Options();
        options.addOption(ProgramOptions.USER_ID, true, "Only run the analysis for specific user");
        options.addOption(ProgramOptions.KEY_FILE, true, "Admin key file of the Firebase account");
        options.addOption(ProgramOptions.SAME_DAY_START_POINT, true, "Starring point of the day in hours");
        options.addOption(ProgramOptions.STILL_EVENT_MERGE_THRESHOLD, true, "Still event merging " +
                "threshold. By default it is 2 minutes.");
        options.addOption(ProgramOptions.WALKING_RUNNING_EVENT_MERGE_THRESHOLD, true, "Walking and " +
                "running events merging threshold. By default it is 2 minutes.");
        options.addOption(ProgramOptions.NO_MERGE_STILL, false, "Do not merge still events");
        options.addOption(ProgramOptions.NO_MERGE_WALKING_RUNNING, false, "Do not merge waling and running events");
        options.addOption(ProgramOptions.START_DATE, true, "Start date (mm-dd-yyyy) to filter data collection based on a date range.");
        options.addOption(ProgramOptions.END_DATE, true, "End date (mm-dd-yyyy) to filter data collection based on a date range.");
        options.addOption(ProgramOptions.SAVE_ON_PATH, true, "Path of directory to save output data on.");
        options.addOption(ProgramOptions.SKIP_KMZ, false, "No export data in KMZ format.");
        options.addOption(ProgramOptions.MULTI_USERS_PATH, true, "Path to file including multiple user IDs.");
        return options;
    }
}
