package com.zhanyage.asm;

import com.zhanyage.asm.item.TraceMethod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by zhanyage on 2021/2/22
 * Describe:
 */
public class MethodTracer {

    private static final String TAG = "Matrix.MethodTracer";
    private static AtomicInteger traceMethodCount = new AtomicInteger();
    private AsmBuildConfig asmBuildConfig;
    private final HashMap<String, TraceMethod> mCollectMethodMap;
    private final HashMap<String, String> mCollectedClassExtendMap;

    public MethodTracer(AsmBuildConfig config, HashMap<String, TraceMethod> collectedMap, HashMap<String, String> collectedClassExtendMap) {
        this.asmBuildConfig = config;
        this.mCollectedClassExtendMap = collectedClassExtendMap;
        this.mCollectMethodMap = collectedMap;
        Log.i(TAG, "collectMethodMap size:" + mCollectMethodMap.size());
    }

    public void trace(Map<File, File> srcFolderList, Map<File, File> dependencyJarList) {
        traceMethodFromSrc(srcFolderList);
        traceMethodFromJar(dependencyJarList);
    }

    private void traceMethodFromSrc(Map<File, File> srcMap) {
        if (null != srcMap) {
            for (Map.Entry<File, File> entry : srcMap.entrySet()) {
                innerTraceMethodFromSrc(entry.getKey(), entry.getValue());
            }
        }
    }

    private void traceMethodFromJar(Map<File, File> dependencyMap) {
        if (null != dependencyMap) {
            for (Map.Entry<File, File> entry : dependencyMap.entrySet()) {
                innerTraceMethodFromJar(entry.getKey(), entry.getValue());
            }
        }
        Log.e(TAG, "traceMethodFromJar is end!");
    }

    private void innerTraceMethodFromSrc(File input, File output) {
        ArrayList<File> classFileList = new ArrayList<>();
        if (input.isDirectory()) {
            listClassFiles(classFileList, input);
        } else {
            classFileList.add(input);
        }
        for (File classFile : classFileList) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                final String changedFileInputFullPath = classFile.getAbsolutePath();
                final File changedFileOutput = new File(changedFileInputFullPath.replace(input.getAbsolutePath(), output.getAbsolutePath()));
                if (!changedFileOutput.exists()) {
                    changedFileOutput.getParentFile().mkdirs();
                }
                changedFileOutput.createNewFile();
                if (asmBuildConfig.isNeedTraceClass(classFile.getName())) {
                    is = new FileInputStream(classFile);
                    ClassReader classReader = new ClassReader(is);
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS); //自动计算栈大小和本地方法数
                    ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    is.close();

                    //区分增量编译和非增量编译
                    if (output.isDirectory()) {
                        os = new FileOutputStream(changedFileOutput);
                    } else {
                        os = new FileOutputStream(output);
                    }
                    os.write(classWriter.toByteArray());
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                    os.close();
                } catch (Exception e) {
                    //ignore
                }
            }
        }
    }

    private void innerTraceMethodFromJar(File input, File output) {
        ZipOutputStream zipOutputStream = null;
        ZipFile zipFile = null;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
            zipFile = new ZipFile(input);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();
                if (asmBuildConfig.isNeedTraceClass(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ClassReader classReader = new ClassReader(inputStream);
                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                    ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    byte[] data = classWriter.toByteArray();
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    Util.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream);
                } else {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    Util.addZipEntry(zipOutputStream, newZipEntry, inputStream);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "[traceMethodFromJar] err! %s", output.getAbsolutePath());
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    zipOutputStream.close();
                }
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "close stream err!");
            }
        }
    }

    private void listClassFiles(ArrayList<File> classFiles, File folder) {
        File[] files = folder.listFiles();
        if (null == files) {
            Log.e(TAG, "[listClassFiles] files is null! %s", folder.getAbsolutePath());
            return;
        }
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (file.isDirectory()) {
                listClassFiles(classFiles, file);
            } else {
                if (null != file && file.isFile()) {
                    classFiles.add(file);
                }
            }
        }
    }

    private class TraceClassAdapter extends ClassVisitor {

        private String className;
        private boolean isABSClass = false;
        private boolean isMethodBeatClass = false;

        public TraceClassAdapter(int api, ClassVisitor cv) {
            super(api, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
            if ((access & Opcodes.ACC_ABSTRACT) > 0 || (access & Opcodes.ACC_INTERFACE) > 0) {
                isABSClass = true;
            }
            if (asmBuildConfig.isMethodBeatClass(className, mCollectedClassExtendMap)) {
                isMethodBeatClass = true;
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (isABSClass) {
                return super.visitMethod(access, name, desc, signature, exceptions);
            } else {
                MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                return new TraceMethodAdapter(api, methodVisitor, access, name, desc, this.className, isMethodBeatClass);
            }
        }
    }

    private class TraceMethodAdapter extends AdviceAdapter {
        private String methodName;
        private boolean isMethodBeatClass;
        private String className;
        private String name;


        protected TraceMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className,
                                     boolean isMethodBeatClass) {
            super(api, mv, access, name, desc);
            TraceMethod traceMethod = TraceMethod.create(0, access, className, name, desc);
            this.methodName = traceMethod.getMethodName();
            this.isMethodBeatClass = isMethodBeatClass;
            this.className = className;
            this.name = name;
        }

        @Override
        protected void onMethodEnter() {
            TraceMethod traceMethod = mCollectMethodMap.get(methodName);
            if (traceMethod != null) {
                traceMethodCount.incrementAndGet();
                //裁剪方法名字
                String sectionName = methodName;
                int length = sectionName.length();
                if (length > AsmBuildConstants.MAX_SECTION_NAME_LEN) {
                    // 先去掉参数
                    int parmIndex = sectionName.indexOf('(');
                    sectionName = sectionName.substring(0, parmIndex);
                    // 如果依然更大，直接裁剪
                    length = sectionName.length();
                    if (length > AsmBuildConstants.MAX_SECTION_NAME_LEN) {
                        sectionName = sectionName.substring(length - AsmBuildConstants.MAX_SECTION_NAME_LEN);
                    }
                }
                mv.visitLdcInsn(className);
                mv.visitLdcInsn(sectionName + "is start");
                mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false);
            }
        }

        @Override
        protected void onMethodExit(int opcode) {
            TraceMethod traceMethod = mCollectMethodMap.get(methodName);
            if (traceMethod != null) {
                traceMethodCount.incrementAndGet();
                String sectionName = methodName;
                int length = sectionName.length();
                if (length > AsmBuildConstants.MAX_SECTION_NAME_LEN) {
                    // 先去掉参数
                    int parmIndex = sectionName.indexOf('(');
                    sectionName = sectionName.substring(0, parmIndex);
                    // 如果依然更大，直接裁剪
                    length = sectionName.length();
                    if (length > AsmBuildConstants.MAX_SECTION_NAME_LEN) {
                        sectionName = sectionName.substring(length - AsmBuildConstants.MAX_SECTION_NAME_LEN);
                    }
                }
                mv.visitLdcInsn(className);
                mv.visitLdcInsn(sectionName + "is end");
                mv.visitMethodInsn(INVOKESTATIC, "android/util/Log", "i", "(Ljava/lang/String;Ljava/lang/String;)I", false);
            }
        }
    }

}
