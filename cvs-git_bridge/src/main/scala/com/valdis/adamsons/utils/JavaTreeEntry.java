package com.valdis.adamsons.utils;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;

public class JavaTreeEntry {
	private final String pathString;
	private final FileMode fileMode;
	private final ObjectId objectId;
	
	public JavaTreeEntry(String pathString, FileMode fileMode, ObjectId objectId) {
		this.pathString = pathString;
		this.fileMode = fileMode;
		this.objectId = objectId;
	}

	public String getPathString() {
		return pathString;
	}

	public FileMode getFileMode() {
		return fileMode;
	}

	public ObjectId getObjectId() {
		return objectId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((fileMode == null) ? 0 : fileMode.hashCode());
		result = prime * result
				+ ((objectId == null) ? 0 : objectId.hashCode());
		result = prime * result
				+ ((pathString == null) ? 0 : pathString.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JavaTreeEntry other = (JavaTreeEntry) obj;
		if (fileMode == null) {
			if (other.fileMode != null)
				return false;
		} else if (!fileMode.equals(other.fileMode))
			return false;
		if (objectId == null) {
			if (other.objectId != null)
				return false;
		} else if (!objectId.equals(other.objectId))
			return false;
		if (pathString == null) {
			if (other.pathString != null)
				return false;
		} else if (!pathString.equals(other.pathString))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "JavaTreeEntry(" + pathString + ", " + fileMode + ", "
				+ objectId + ")";
	}
}
