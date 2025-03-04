package io.github.awidesky.YoutubeClipboardAutoDownloader.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.awidesky.guiUtil.Logger;

public class OSUtil {

	private static final String OS = System.getProperty("os.name").toLowerCase();

	public static boolean isWindows() { return OS.contains("windows"); }
	public static boolean isMac() { return OS.contains("mac"); }
	public static boolean isLinux() { return OS.contains("linux"); }


	public static void addExecutePermission(Path file, Logger log) {
		if(isWindows()) {
			addWindowsPermission(file, log, AclEntryPermission.EXECUTE);
		} else {
			addPosixPermission(file, log, PosixFilePermission.OWNER_EXECUTE);
		}
	}
	public static boolean addDeletePermissionRecursive(Path dir, Logger log) {
		Predicate<Path> addPerm = isWindows() ?
			d -> addWindowsPermission(d, log, AclEntryPermission.DELETE_CHILD, AclEntryPermission.WRITE_DATA)
			:
			d -> addPosixPermission(d, log, 
					PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
		try {
			return Files.walk(dir).filter(Files::isDirectory).allMatch(addPerm);
		} catch(IOException e) {
			log.log(e);
			log.log("Failed to traverse : " + dir);
			return false;
		}
	}
	
	
	private static boolean addWindowsPermission(Path file, Logger log, AclEntryPermission... perm) {
		List<AclEntryPermission> addPerm = Arrays.asList(perm);
		try {
			UserPrincipal user = file.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
			log.log("Current user : " + user);
			printACL(file, log);

			AclFileAttributeView aclAttr = Files.getFileAttributeView(file, AclFileAttributeView.class);
			List<AclEntry> list = aclAttr.getAcl();

			Optional<UserPrincipal> result = Stream.of(//Try current JVM process user first, and the file owner if failed or the user already has the permission.
					user,
					aclAttr.getOwner()
					).filter(principal -> {
						AclEntry entryOfThePrincipal = list.stream().filter(x -> x.principal().equals(principal)).findFirst()
								.orElseGet(() -> AclEntry.newBuilder().setPrincipal(principal).setType(AclEntryType.ALLOW).build());

						Set<AclEntryPermission> permissions = entryOfThePrincipal.permissions();
						if(permissions.containsAll(addPerm)) {
							//this user already has the permission, check the owner.
							return false;
						}

						permissions.addAll(addPerm);

						list.remove(entryOfThePrincipal);
						list.add(AclEntry.newBuilder(entryOfThePrincipal).setPermissions(permissions).build());

						try {
							aclAttr.setAcl(list);
							return true;
						} catch (IOException e) {
							log.log(e);
							return false;
						}
					}).findFirst();
			
			result.ifPresentOrElse(
							u -> log.log("Execute permission added for user :" + u),
							() -> log.log("Failed to add execute permission!")
							);

			printACL(file, log);
			return result.isPresent();
		} catch (IOException e) {
			log.log(e);
			log.log("Failed to add permission \"%s\""
					.formatted(addPerm.stream().map(AclEntryPermission::toString).collect(Collectors.joining(", "))));
			return false;
		}
	}

	private static boolean addPosixPermission(Path file, Logger log, PosixFilePermission... perms) {
		List<PosixFilePermission> toAdd = Arrays.asList(perms);
		try {
			Set<PosixFilePermission> permSet = Files.getPosixFilePermissions(file);

			log.log("Current user : " + file.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name")));
			log.log("Permission of " + file + " : " + PosixFilePermissions.toString(permSet));
			log.log("Owner : " + Files.getOwner(file));

			permSet.addAll(toAdd);
			Files.setPosixFilePermissions(file, permSet);

			permSet = Files.getPosixFilePermissions(file);
			log.log("After adding permission(s) \"%s\" : %s".formatted(
					toAdd.stream().map(PosixFilePermission::toString).collect(Collectors.joining(", ")),
					PosixFilePermissions.toString(permSet)));
			
			return permSet.containsAll(toAdd);
		} catch(IOException e) {
			log.log(e);
			log.log("Failed to add posix permission(s) \"%s\"".formatted(
					toAdd.stream().map(PosixFilePermission::toString).collect(Collectors.joining(", "))));
			return false;
		}
	}

	private static void printACL(Path file, Logger log) throws IOException {
		log.log("Permission of " + file + " : ");
		AclFileAttributeView aclAttr = Files.getFileAttributeView(file, AclFileAttributeView.class);
		log.log("Owner : " + aclAttr.getOwner());
		for (AclEntry aclEntry : aclAttr.getAcl()) {
			log.log();
			log.log("User : " + aclEntry.principal() + ", type : " + aclEntry.type().name());
			log.log("Permmission : " + aclEntry.permissions().stream().map(AclEntryPermission::name).collect(Collectors.joining(", ")));
			log.log("Flag : " + aclEntry.flags().stream().map(AclEntryFlag::name).collect(Collectors.joining(", ")));
		}
	}

}
