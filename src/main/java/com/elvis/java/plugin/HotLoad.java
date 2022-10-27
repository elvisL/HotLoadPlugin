package com.elvis.java.plugin;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.tools.jdi.SocketAttachingConnector;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jetbrains.annotations.Nullable;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 热加载
 */
public class HotLoad {

    /**
     * 热部署入口
     * @param host
     * @param port
     * @param className
     * @param classFile
     * @param jacoco 应对有些应用开启了覆盖率检测类似的agent，保持结构一致，避免热加载不兼容
     */
    public static void hotLoadByRedefine(String host, String port,String className, String classFile, boolean jacoco) {

        VirtualMachine vm = getVirtualMachine(host,port);

        if (vm == null) return;

        try {

            byte[] classBytes = getClassBytes(classFile,className,jacoco);
            if (classBytes == null) return;

            ReferenceType referenceType = getReferenceType(vm,className);
            Map<ReferenceType, byte[]> classToBytes = new HashMap<>();
            classToBytes.put(referenceType, classBytes);

            vm.redefineClasses(classToBytes);

        } catch (Throwable e) {
            Notifier.notifyError("hot load fail on "+host+e.getLocalizedMessage());
        } finally {
            Notifier.notify("hot load success on "+host);
            vm.dispose();
        }
    }

    public static ReferenceType getReferenceType(VirtualMachine vm, String className){

        List<ReferenceType> referenceTypes = vm.classesByName(className);
        if (referenceTypes == null || referenceTypes.size() == 0) {
            Notifier.notifyError("The target class is null.");
            return null;
        }
        if (referenceTypes.size() > 1) {
            Notifier.notifyError("Find more then one class. Please check your class name.");
            return null;
        }

        return referenceTypes.get(0);
    }

    @Nullable
    private static VirtualMachine getVirtualMachine(String host, String port) {

        VirtualMachine vm = connect(host, port, "2000");
        if (vm == null) {
            Notifier.notifyError("can't connect target jvm,please configure such as : Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000");
            return null;
        }
        boolean canRedefineClasses = vm.canRedefineClasses();
        if (!canRedefineClasses) {
            Notifier.notifyError("Target vm can not support RedefineClasses.");
            return null;
        }
        return vm;
    }


    /**
     * 获取新的字节码
     * @param classFile
     * @param className
     * @param jacoco
     * @return
     * @throws IOException
     */
    private static byte[] getClassBytes(String classFile, String className, boolean jacoco) throws IOException {

        if (classFile == null || classFile.trim().isEmpty()) {
            Notifier.notifyError("Class file is empty");
            return null;
        }

        File f = new File(classFile);
        if (!f.exists()) {
            Notifier.notifyError("can't find class on build path,please compile before");
            return null;
        }
        BasicFileAttributes basicFileAttributes = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
        if (!basicFileAttributes.isRegularFile()) {
            Notifier.notifyError("can't find class on build path,please compile before");
            return null;
        }

        BufferedInputStream in;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream((int) basicFileAttributes.size())) {

            in = new BufferedInputStream(Files.newInputStream(f.toPath()));
            int bufSize = 1024;
            byte[] buffer = new byte[bufSize];
            int len;
            while (-1 != (len = in.read(buffer, 0, bufSize))) {
                bos.write(buffer, 0, len);
            }

            byte[] classBytes = bos.toByteArray();

            if (jacoco) {
                LoggerRuntime runtime = new LoggerRuntime();
                Instrumenter instrumenter = new Instrumenter(runtime);
                classBytes = instrumenter.instrument(classBytes, className);
            }

            return classBytes;
        } catch (IOException e) {
            Notifier.notifyError("file read error " + e.getLocalizedMessage());
        }
        return null;
    }


    public static VirtualMachine connect(String host, String port, String connectTimeout) {
        VirtualMachine vm;
        try {
            List<AttachingConnector> allConnectors = Bootstrap.virtualMachineManager().attachingConnectors();

            SocketAttachingConnector socketAttachingConnector = null;
            for (Connector connector : allConnectors) {
                if (connector instanceof SocketAttachingConnector) {
                    socketAttachingConnector = (SocketAttachingConnector) connector;
                    break;
                }
            }
            if (socketAttachingConnector == null) {
                Notifier.notifyError("Can not find SocketAttachingConnector");
                return null;
            }

            Map<String, Connector.Argument> arguments1 = socketAttachingConnector.defaultArguments();
            Connector.Argument hostArg = arguments1.get("hostname");
            Connector.Argument portArg = arguments1.get("port");
            Connector.Argument timeArg = arguments1.get("timeout");
            hostArg.setValue(host);
            portArg.setValue(port);
            timeArg.setValue(connectTimeout);

            Notifier.notify("Connecting to target vm, host:" + host + " port:" + port +" success");
            vm = socketAttachingConnector.attach(arguments1);
        } catch (Throwable e) {
            Notifier.notifyError("Connecting to target vm error :" +e.getMessage());
            return null;
        }
        return vm;
    }

}
