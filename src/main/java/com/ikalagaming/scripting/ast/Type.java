package com.ikalagaming.scripting.ast;

import lombok.Getter;
import lombok.NonNull;

/**
 * A type so that we can have a rudimentary type system and interface with Java
 * types.
 *
 * @author Ches Burks
 *
 */
@Getter
public class Type {

	/**
	 * The base kind of type, ignoring array values or specific identifier
	 * values.
	 *
	 * @author Ches Burks
	 *
	 */
	public enum Base {
		/**
		 * A node that does not evaluate to a type.
		 */
		VOID,
		/**
		 * A node that is treated as a boolean.
		 */
		BOOLEAN,
		/**
		 * A node that is treated as a character.
		 */
		CHAR,
		/**
		 * A node that is treated as a double.
		 */
		DOUBLE,
		/**
		 * A node that is treated as an integer.
		 */
		INT,
		/**
		 * A node that is treated as an identifier, a class or interface type.
		 */
		IDENTIFIER,
		/**
		 * A node that is treated as a string.
		 */
		STRING;
	}

	/**
	 * Checks that the given type is a primitive one, and throw an exception if
	 * not.
	 *
	 * @param base The base type.
	 * @throws IllegalArgumentException If not a primitive type.
	 */
	private static void checkForPrimitives(@NonNull Base base) {
		switch (base) {
			case BOOLEAN:
			case CHAR:
			case DOUBLE:
			case INT:
			case STRING:
				break;
			case IDENTIFIER:
			case VOID:
			default:
				throw new IllegalArgumentException(
					base.toString() + " is not a primitive type.");
		}
	}

	/**
	 * Create a type representing an identifier.
	 *
	 * @param value The literal name of the identifier, like "Player.Inventory",
	 *            or "Object".
	 * @return The newly created type.
	 */
	public static Type identifier(@NonNull String value) {
		return new Type(Base.IDENTIFIER, value, 0);
	}

	/**
	 * Create a type representing an array of identifiers.
	 *
	 * @param value The literal name of the identifier, like "Player.Inventory",
	 *            or "Object".
	 * @param dimensions The depth of the array.
	 * @return The newly created type.
	 */
	public static Type identifierArray(@NonNull String value, int dimensions) {
		return new Type(Base.IDENTIFIER, value, dimensions);
	}

	/**
	 * Create a type representing a primitive.
	 *
	 * @param base The base type.
	 * @return The newly created type.
	 * @throws IllegalArgumentException If not a primitive type.
	 */
	public static Type primitive(@NonNull Base base) {
		Type.checkForPrimitives(base);
		return new Type(base, "", 0);
	}

	/**
	 * Create a type representing an array of primitives.
	 *
	 * @param base The base type.
	 * @param dimensions The depth of the array.
	 * @return The newly created type.
	 * @throws IllegalArgumentException If not a primitive type.
	 */
	public static Type primitiveArray(@NonNull Base base, int dimensions) {
		Type.checkForPrimitives(base);
		return new Type(base, "", dimensions);
	}

	/**
	 * Create a Void type.
	 *
	 * @return The newly created type.
	 */
	public static Type voidType() {
		return new Type(Base.VOID, "", 0);
	}

	/**
	 * The base type, specifics of whether it's an array or specific identifier
	 * type aside.
	 *
	 * @return The type.
	 */
	private final Base base;

	/**
	 * The actual value of the type if it's an identifier.
	 *
	 * @return The value of the type, or an empty string if not an identifier.
	 */
	private final String value;

	/**
	 * The depth of the array, if this is an array type. A value of 0 would be
	 * used if it is not an array, 1 would be for something like "int[]", and 2
	 * would be for something like "int[][]".
	 *
	 * @return An integer greater than or equal to zero representing how deep of
	 *         an array this represents.
	 */
	private final int dimensions;

	/**
	 * Construct a new Type.
	 *
	 * @param base The base type.
	 * @param value The value of the identifier.
	 * @param dimensions The dimensions.
	 */
	private Type(@NonNull Base base, @NonNull String value, int dimensions) {
		this.base = base;
		this.value = value;
		if (dimensions < 0) {
			throw new IllegalArgumentException("Dimensions must be positive.");
		}
		this.dimensions = dimensions;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (this.base == Base.IDENTIFIER) {
			result.append(this.value);
		}
		else {
			result.append(this.base.toString());
		}
		if (this.dimensions > 0) {
			for (int i = 0; i < this.dimensions; ++i) {
				result.append("[]");
			}
		}
		return result.toString();
	}
}
