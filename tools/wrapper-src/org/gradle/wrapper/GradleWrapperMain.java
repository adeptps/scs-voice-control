package org.gradle.wrapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lightweight Gradle wrapper used only for this repository.
 *
 * It downloads the Gradle distribution declared in gradle-wrapper.properties and executes it.
 */
public final class GradleWrapperMain {

    public static void main(String[] args) {
        try {
            File projectDir = new File(System.getProperty("user.dir"));
            File propsFile = new File(projectDir, "gradle/wrapper/gradle-wrapper.properties");
            if (!propsFile.exists()) {
                throw new IllegalStateException("gradle-wrapper.properties not found: " + propsFile.getAbsolutePath());
            }

            Properties props = new Properties();
            try (InputStream in = new FileInputStream(propsFile)) {
                props.load(in);
            }

            String distributionUrl = props.getProperty("distributionUrl");
            if (distributionUrl == null || distributionUrl.isBlank()) {
                throw new IllegalStateException("distributionUrl is missing in gradle-wrapper.properties");
            }

            String gradleUserHome = System.getenv("GRADLE_USER_HOME");
            if (gradleUserHome == null || gradleUserHome.isBlank()) {
                gradleUserHome = new File(System.getProperty("user.home"), ".gradle").getAbsolutePath();
            }

            File distsBase = new File(gradleUserHome, "wrapper/dists");
            if (!distsBase.exists() && !distsBase.mkdirs()) {
                throw new IOException("Failed to create directory: " + distsBase.getAbsolutePath());
            }

            String fileName = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1);
            if (!fileName.endsWith(".zip")) {
                throw new IllegalStateException("Only .zip distributions are supported: " + distributionUrl);
            }

            String distId = fileName.replace(".zip", "");
            String urlHash = sha256Short(distributionUrl);

            File distDir = new File(new File(distsBase, distId), urlHash);
            File zipFile = new File(distDir, fileName);

            if (!zipFile.exists()) {
                distDir.mkdirs();
                download(distributionUrl, zipFile);
            }

            File marker = new File(distDir, ".unzipped");
            if (!marker.exists()) {
                unzip(zipFile, distDir);
                writeText(marker, "ok");
            }

            File gradleHome = findGradleHome(distDir, distId);
            File gradleExec = resolveGradleExecutable(gradleHome);

            ProcessBuilder pb = new ProcessBuilder();
            String[] cmd = new String[args.length + 1];
            cmd[0] = gradleExec.getAbsolutePath();
            System.arraycopy(args, 0, cmd, 1, args.length);

            pb.command(cmd);
            pb.directory(projectDir);
            pb.inheritIO();

            Process p = pb.start();
            int exit = p.waitFor();
            System.exit(exit);

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void download(String url, File out) throws IOException {
        System.out.println("Downloading Gradle distribution: " + url);
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        try (InputStream in = conn.getInputStream()) {
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            conn.disconnect();
        }
    }

    private static void unzip(File zipFile, File outDir) throws IOException {
        System.out.println("Unzipping: " + zipFile.getName());
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            while (true) {
                ZipEntry entry = zis.getNextEntry();
                if (entry == null) break;

                File out = new File(outDir, entry.getName());

                String canonicalOutDir = outDir.getCanonicalPath() + File.separator;
                String canonicalOutPath = out.getCanonicalPath();
                if (!canonicalOutPath.startsWith(canonicalOutDir)) {
                    throw new IOException("Zip path traversal detected: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    out.mkdirs();
                } else {
                    File parent = out.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(out))) {
                        byte[] buf = new byte[64 * 1024];
                        int r;
                        while ((r = zis.read(buf)) > 0) {
                            fos.write(buf, 0, r);
                        }
                    }
                }

                zis.closeEntry();
            }
        }
    }

    private static File findGradleHome(File distDir, String distId) {
        // Gradle zip usually contains a single top-level directory, e.g. gradle-8.7
        File candidate = new File(distDir, distId);
        if (candidate.exists()) return candidate;

        File[] children = distDir.listFiles();
        if (children != null) {
            for (File c : children) {
                if (c.isDirectory() && c.getName().startsWith("gradle-")) {
                    return c;
                }
            }
        }
        throw new IllegalStateException("Gradle home directory not found in: " + distDir.getAbsolutePath());
    }

    private static File resolveGradleExecutable(File gradleHome) {
        boolean windows = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        File bin = new File(gradleHome, "bin");
        File exe = new File(bin, windows ? "gradle.bat" : "gradle");
        if (!exe.exists()) {
            throw new IllegalStateException("Gradle executable not found: " + exe.getAbsolutePath());
        }
        if (!windows) {
            exe.setExecutable(true);
        }
        return exe;
    }

    private static String sha256Short(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.substring(0, 12);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeText(File f, String text) throws IOException {
        f.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(text);
        }
    }
}
