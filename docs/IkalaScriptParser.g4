parser grammar IkalaScriptParser;

options {
    tokenVocab=IkalaScriptLexer;
}

literal
	:	IntegerLiteral
	|	FloatingPointLiteral
	|	BooleanLiteral
	|	CharacterLiteral
	|	StringLiteral
	|	NullLiteral
	;

// Types, Values, and Variables

primitiveType
	:	numericType
	|	BOOLEAN
	|	STRING
	;

numericType
	:	INT
	|	CHAR
	|	DOUBLE
	;

referenceType
	:	classOrInterfaceType
	|	Identifier
	|	arrayType
	;

classOrInterfaceType
	:	Identifier (DOT Identifier)*
	;

arrayType
	:	primitiveType dims
	|	classOrInterfaceType dims
	|	Identifier dims
	;

dims
	:	LBRACK RBRACK (LBRACK RBRACK)*
	;

variableDeclaratorList
	:	variableDeclarator (COMMA variableDeclarator)*
	;

variableDeclarator
	:	variableDeclaratorId (ASSIGN expression)?
	;

variableDeclaratorId
	:	Identifier dims?
	;

type
	:	primitiveType
	|	referenceType
	;

// Names

typeName
	:	Identifier (DOT Identifier)*
	;

// Scripts

compilationUnit
	:	blockStatement* EOF
	;

// Blocks and Statements

block
	:	LBRACE blockStatements? RBRACE
	;

blockStatements
	:	blockStatement+
	;

blockStatement
	:	localVariableDeclaration
	|	statement
	|	label
	;

localVariableDeclaration
	:	FINAL? type variableDeclaratorList
	;

statement
	:	statementWithoutTrailingSubstatement
	|	labeledStatement
	|	ifThenStatement
	|	ifThenElseStatement
	|	whileStatement
	|	forStatement
	;

statementNoShortIf
	:	statementWithoutTrailingSubstatement
	|	labeledStatementNoShortIf
	|	ifThenElseStatementNoShortIf
	|	whileStatementNoShortIf
	|	forStatementNoShortIf
	;

statementWithoutTrailingSubstatement
	:	block
	|	statementExpression
	|	switchStatement
	|	doStatement
	|	breakStatement
	|	continueStatement
	|	gotoStatement
	|	exitStatement
	;

label
	: Identifier COLON
	;

labeledStatement
	:	label statement
	;

labeledStatementNoShortIf
	:	label statementNoShortIf
	;

statementExpression
	:	assignment
	|	preIncrementExpression
	|	preDecrementExpression
	|	postIncrementExpression
	|	postDecrementExpression
	|	methodInvocation
	;

ifThenStatement
	:	IF LPAREN expression RPAREN statement
	;

ifThenElseStatement
	:	IF LPAREN expression RPAREN statementNoShortIf ELSE statement
	;

ifThenElseStatementNoShortIf
	:	IF LPAREN expression RPAREN statementNoShortIf ELSE statementNoShortIf
	;

switchStatement
	:	SWITCH LPAREN expression RPAREN switchBlock
	;

switchBlock
	:	LBRACE switchBlockStatementGroup* switchLabel* RBRACE
	;

switchBlockStatementGroup
	:	switchLabel+ blockStatements
	;

switchLabel
	:	CASE expression COLON
	|	DEFAULT COLON
	;

whileStatement
	:	WHILE LPAREN expression RPAREN statement
	;

whileStatementNoShortIf
	:	WHILE LPAREN expression RPAREN statementNoShortIf
	;

doStatement
	:	DO statement WHILE LPAREN expression RPAREN
	;

forStatement
	:	basicForStatement
	|	enhancedForStatement
	;

forStatementNoShortIf
	:	basicForStatementNoShortIf
	|	enhancedForStatementNoShortIf
	;

basicForStatement
	:	FOR LPAREN forInit? SEMICOLON expression? SEMICOLON statementExpressionList? RPAREN statement
	;

basicForStatementNoShortIf
	:	FOR LPAREN forInit? SEMICOLON expression? SEMICOLON statementExpressionList? RPAREN statementNoShortIf
	;

forInit
	:	statementExpressionList
	|	localVariableDeclaration
	;

statementExpressionList
	:	statementExpression (COMMA statementExpression)*
	;

enhancedForStatement
	:	FOR LPAREN FINAL? type variableDeclaratorId COLON expression RPAREN statement
	;

enhancedForStatementNoShortIf
	:	FOR LPAREN FINAL? type variableDeclaratorId COLON expression RPAREN statementNoShortIf
	;

breakStatement
	:	BREAK Identifier?
	;

continueStatement
	:	CONTINUE Identifier?
	;

gotoStatement
	:	GOTO Identifier
	;

exitStatement
	:	EXIT
	;

// Expressions

primary
	:	(primary_LHS) (primary_extension)*
	;

arrayAccess_LHS_General
	:	literal
	|	LPAREN expression RPAREN
	|	fieldAccess
	|	methodInvocation
	;

primary_extension
	:	fieldAccess_extension
	|	arrayAccess_extension
	|	methodInvocation_extension
	;

primary_extension_access
	:	fieldAccess_extension
	|	methodInvocation_extension
	;

primary_LHS
	:	literal
	|	LPAREN expression RPAREN
	|	arrayAccess_LHS
	|	methodInvocation_LHS
	;

primary_LHS_access
	:	literal
	|	LPAREN expression RPAREN
	|	methodInvocation_LHS
	;

fieldAccess
	:	primary DOT Identifier
	;

fieldAccess_extension
	:	DOT Identifier
	;

arrayAccess
	:	typeName (LBRACK expression RBRACK)+
	|	arrayAccess_LHS_General (LBRACK expression RBRACK)+
	;

arrayAccess_extension
	:	primary_extension_access (LBRACK expression RBRACK)+
	;

arrayAccess_LHS
	:	typeName (LBRACK expression RBRACK)+
	|	primary_LHS_access (LBRACK expression RBRACK)+
	;

methodInvocation
	:	typeName LPAREN argumentList? RPAREN
	|	primary DOT Identifier LPAREN argumentList? RPAREN
	;

methodInvocation_extension
	:	DOT Identifier LPAREN argumentList? RPAREN
	;

methodInvocation_LHS
	:	typeName LPAREN argumentList? RPAREN
	;

argumentList
	:	expression (COMMA expression)*
	;

expression
	:	conditionalExpression
	|	assignment
	;

assignment
	:	leftHandSide assignmentOperator expression
	;

leftHandSide
	:	Identifier
	|	fieldAccess
	|	arrayAccess
	;

assignmentOperator
	:	ASSIGN
	|	MUL_ASSIGN
	|	DIV_ASSIGN
	|	MOD_ASSIGN
	|	ADD_ASSIGN
	|	SUB_ASSIGN
	;

conditionalExpression
	:	conditionalOrExpression
	|	conditionalOrExpression QUESTION expression COLON conditionalExpression
	;

conditionalOrExpression
	:	conditionalAndExpression
	|	conditionalOrExpression OR conditionalAndExpression
	;

conditionalAndExpression
	:	equalityExpression
	|	conditionalAndExpression AND equalityExpression
	;

equalityExpression
	:	relationalExpression
	|	equalityExpression EQUAL relationalExpression
	|	equalityExpression NOTEQUAL relationalExpression
	;

relationalExpression
	:	additiveExpression
	|	relationalExpression LT additiveExpression
	|	relationalExpression GT additiveExpression
	|	relationalExpression LTE additiveExpression
	|	relationalExpression GTE additiveExpression
	;

additiveExpression
	:	multiplicativeExpression
	|	additiveExpression ADD multiplicativeExpression
	|	additiveExpression SUB multiplicativeExpression
	;

multiplicativeExpression
	:	unaryExpression
	|	multiplicativeExpression MUL unaryExpression
	|	multiplicativeExpression DIV unaryExpression
	|	multiplicativeExpression MOD unaryExpression
	;

unaryExpression
	:	preIncrementExpression
	|	preDecrementExpression
	|	ADD unaryExpression
	|	SUB unaryExpression
	|	unaryExpressionNotPlusMinus
	;

preIncrementExpression
	:	INC unaryExpression
	;

preDecrementExpression
	:	DEC unaryExpression
	;

unaryExpressionNotPlusMinus
	:	postfixExpression
	|	NOT unaryExpression
	|	castExpression
	;

postfixExpression
	:	(primary | typeName) (INC | DEC)*
	;

postIncrementExpression
	:	postfixExpression INC
	;

postDecrementExpression
	:	postfixExpression DEC
	;

castExpression
	:	LPAREN primitiveType RPAREN unaryExpression
	|	LPAREN referenceType RPAREN unaryExpressionNotPlusMinus
	;