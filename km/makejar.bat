del /Q RemoteMaster.jar
jar cmf RemoteMaster.mf RemoteMaster.jar -C classes com/hifiremote/jp1 info/clearthought/layout/Table*.class
del /Q Setup.jar
jar cmf Setup.mf Setup.jar -C classes Setup.class
