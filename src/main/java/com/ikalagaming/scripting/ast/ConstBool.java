package com.ikalagaming.scripting.ast;

import lombok.Getter;
import lombok.Setter;

/**
 * A boolean literal.
 * 
 * @author Ches Burks
 *
 */
@Getter
@Setter
public class ConstBool extends Node {
	private boolean value;

	@Override
	public String toString() {
		return Boolean.toString(value);
	}
}
