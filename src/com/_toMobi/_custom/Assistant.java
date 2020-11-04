package com._toMobi._custom;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.*;
import java.util.Enumeration;

/**
 * @author edgar
 */
public abstract class Assistant {

    protected InetAddress get_first_nonLoopback_address(boolean preferIpv4, boolean preferIPv6) throws SocketException, UnknownHostException {
        InetAddress result = InetAddress.getLocalHost();
        Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            for (Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses(); inetAddressEnumeration.hasMoreElements(); ) {
                InetAddress inetAddress = inetAddressEnumeration.nextElement();
                if (!inetAddress.isLoopbackAddress()) {
                    if (inetAddress instanceof Inet4Address) {
                        if (preferIPv6) {
                            continue;
                        }
                        result = inetAddress;
                        break;
                    }
                    if (inetAddress instanceof Inet6Address) {
                        if (preferIpv4) {
                            continue;
                        }
                        result = inetAddress;
                        break;
                    }
                }
            }
            if (result != null) break;
        }
        return result;
    }

    protected final boolean the_file_has_zero_bytes(@NotNull File file) {
        return get_size_of_the_provided_file_or_folder(file) == 0;
    }

    protected final double get_size_of_the_provided_file_or_folder(@NotNull File file) {
        double bytes = 0;
        if (file.isFile()) {
            bytes = file.length();
        } else {
            if (file.isDirectory()) {
                final File[] fileList = file.listFiles();
                if (fileList != null) {
                    for (File file1 : fileList) {
                        bytes += get_size_of_the_provided_file_or_folder(file1);
                    }
                }
            }
        }
        return bytes;
    }

    protected final @NotNull String make_bytes_more_presentable(double bytes) {
        final double kilobytes = (bytes / 1024);
        final double megabytes = (kilobytes / 1024);
        final double gigabytes = (megabytes / 1024);
        final double terabytes = (gigabytes / 1024);
        final double petabytes = (terabytes / 1024);
        final double exabytes = (petabytes / 1024);
        final double zettabytes = (exabytes / 1024);
        final double yottabytes = (zettabytes / 1024);
        String result;
        if (((int) yottabytes) > 0) {
            result = String.format("%,.2f", yottabytes).concat(" YB");
            return result;
        }
        if (((int) zettabytes) > 0) {
            result = String.format("%,.2f", zettabytes).concat(" ZB");
            return result;
        }
        if (((int) exabytes) > 0) {
            result = String.format("%,.2f", exabytes).concat(" EB");
            return result;
        }
        if (((int) petabytes) > 0) {
            result = String.format("%,.2f", petabytes).concat(" PB");
            return result;
        }
        if (((int) terabytes) > 0) {
            result = String.format("%,.2f", terabytes).concat(" TB");
            return result;
        }
        if (((int) gigabytes) > 0) {
            result = String.format("%,.2f", gigabytes).concat(" GB");
            return result;
        }
        if (((int) megabytes) > 0) {
            result = String.format("%,.2f", megabytes).concat(" MB");
            return result;
        }
        if (((int) kilobytes) > 0) {
            result = String.format("%,.2f", kilobytes).concat(" KB");
            return result;
        }
        result = String.format("%,.0f", bytes).concat(" Bytes");
        return result;
    }

    protected String format_path_name_to_current_os(String myPath) {
        String myOperatingSystemSlash = get_slash_for_my_os();
        if (myOperatingSystemSlash == null) {
            return myPath;
        }
        if (!myPath.contains(myOperatingSystemSlash)) {
            myPath = myPath.replace("\\", myOperatingSystemSlash);
        }
        return myPath;
    }

    String get_slash_for_my_os() {
        String OS = System.getProperty("os.name").toLowerCase();
        if (OS.contains("win")) {
            //windows
            return "\\";
        } else if (OS.contains("mac")) {
            //mac
            return "/";
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix")) {
            //unix
            return "/";
        } else if (OS.contains("sunos")) {
            //solaris
            return "/";
        } else {
            return null;
        }
    }
}
