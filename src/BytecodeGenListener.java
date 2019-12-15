import java.util.Hashtable;

import generated.MiniCParser.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import generated.MiniCBaseListener;
import generated.MiniCParser;


public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
	ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
	SymbolTable symbolTable = new SymbolTable();
	
	int tab = 0;
	int label = 0;

	// program	: decl+
	
	@Override
	public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
		symbolTable.initFunDecl();
		
		String fname = BytecodeGenListenerHelper.getFunName(ctx);
		ParamsContext params;
		
		if (fname.equals("main")) {
			symbolTable.putLocalVar("args", SymbolTable.Type.INTARRAY);
		} else {
			symbolTable.putFunSpecStr(ctx);
			params = (MiniCParser.ParamsContext) ctx.getChild(3);
			symbolTable.putParams(params);
		}		
	}

	
	// var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
	@Override
	public void enterVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		
		if (BytecodeGenListenerHelper.isArrayDecl(ctx)) {
			symbolTable.putGlobalVar(varName, SymbolTable.Type.INTARRAY);
		}
		else if (BytecodeGenListenerHelper.isDeclWithInit(ctx)) {
			symbolTable.putGlobalVarWithInitVal(varName, SymbolTable.Type.INT, BytecodeGenListenerHelper.initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putGlobalVar(varName, SymbolTable.Type.INT);
		}
	}

	
	@Override
	public void enterLocal_decl(MiniCParser.Local_declContext ctx) {			
		if (BytecodeGenListenerHelper.isArrayDecl(ctx)) {
			symbolTable.putLocalVar(BytecodeGenListenerHelper.getLocalVarName(ctx), SymbolTable.Type.INTARRAY);
		}
		else if (BytecodeGenListenerHelper.isDeclWithInit(ctx)) {
			symbolTable.putLocalVarWithInitVal(BytecodeGenListenerHelper.getLocalVarName(ctx), SymbolTable.Type.INT, BytecodeGenListenerHelper.initVal(ctx));
		}
		else  { // simple decl
			symbolTable.putLocalVar(BytecodeGenListenerHelper.getLocalVarName(ctx), SymbolTable.Type.INT);
		}	
	}

	
	@Override
	public void exitProgram(MiniCParser.ProgramContext ctx) {
		String classProlog = BytecodeGenListenerHelper.getFunProlog();
		
		String fun_decl = "", var_decl = "";
		
		for(int i = 0; i < ctx.getChildCount(); i++) {
			if(BytecodeGenListenerHelper.isFunDecl(ctx, i))
				fun_decl += newTexts.get(ctx.decl(i));
			else
				var_decl += newTexts.get(ctx.decl(i));
		}
		
		newTexts.put(ctx, classProlog + var_decl + fun_decl);
		
		System.out.println(newTexts.get(ctx));
	}	
	
	
	// decl	: var_decl | fun_decl
	@Override
	public void exitDecl(MiniCParser.DeclContext ctx) {
		String decl = "";
		if(ctx.getChildCount() == 1)
		{
			if(ctx.var_decl() != null)				//var_decl
				decl += newTexts.get(ctx.var_decl());
			else							//fun_decl
				decl += newTexts.get(ctx.fun_decl());
		}
		newTexts.put(ctx, decl);
	}
	
	// stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
	@Override
	public void exitStmt(MiniCParser.StmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() > 0)
		{
			if(ctx.expr_stmt() != null)				// expr_stmt
				stmt += newTexts.get(ctx.expr_stmt());
			else if(ctx.compound_stmt() != null)	// compound_stmt
				stmt += newTexts.get(ctx.compound_stmt());
			// <(0) Fill here>
			else if(ctx.if_stmt() != null){ // if_stmt()
				stmt += newTexts.get(ctx.if_stmt());
			}
			else if(ctx.while_stmt() != null){ // while_stmt
				stmt += newTexts.get(ctx.while_stmt());
			}
			else{
				stmt += newTexts.get(ctx.return_stmt());
			}
	}
		newTexts.put(ctx, stmt);
	}
	
	// expr_stmt	: expr ';'
	@Override
	public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
		String stmt = "";
		if(ctx.getChildCount() == 2)
		{
			stmt += newTexts.get(ctx.expr());	// expr
		}
		newTexts.put(ctx, stmt);
	}
	
	
	// while_stmt	: WHILE '(' expr ')' stmt
	@Override
	public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
		String stmt = "";

		String condExpr= newTexts.get(ctx.expr());
		String thenStmt = newTexts.get(ctx.stmt().getChild(0));

		String WhileLable = symbolTable.newLabel();
		String lend = symbolTable.newLabel();

		stmt +=  WhileLable + ": "+ "\n"
				+ condExpr + "\n"
				+ "ifeq " + lend + "\n"
				+ thenStmt + "\n"
				+ "goto " + WhileLable + "\n"
				+ lend + ": "  + "\n";

		newTexts.put(ctx,stmt);
			// <(1) Fill here!>
		/*반복문 라벨을 하나 두고 expr을 실행하여 나오는 논리값을 비교하여 0이면 조건에 맞지 않으므로 루프를 빠져나가고
		* 1이면  조건에 맞으므로 thenstmt를 실행한다. 그 뒤 goto문을 이용해 다시 반복문 라벨로 가서 비교를 수행한다.*/
	}
	
	
	@Override
	public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
			String fname = ctx.IDENT().getText();
			String header = funcHeader(ctx,fname);
			String com = newTexts.get(ctx.compound_stmt());
			String Vretrun = "return"+"\n"+".end method"+"\n\n";
			if(BytecodeGenListenerHelper.isVoidF(ctx)){
				newTexts.put(ctx, header+com+Vretrun);
			}
			else{
				newTexts.put(ctx, header+com);
			}
			// <(2) Fill here!>
		/* 함수의 이름을 찾고 그에대한 헤더를 만들어준다 그 뒤 컴파운드 stmt를 이용하여 해당 함수가 가지고 있는
		* 실행문들을 불러온다. 그리고 만약 함수가 void이면 이 함수는 단순한 리턴만하게 되므로 이에대한 문장을
		* 만들어 준뒤 헬퍼에있는 isVoid(ctx)를 이용하여 함수의 리턴타입 확인후 void면 만들어준 문장을 뒤에
		* 넣어준다. */
	}
	

	private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
		return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"	
				+ ".limit stack " 	+ BytecodeGenListenerHelper.getStackSize(ctx) + "\n"
				+ ".limit locals " 	+ BytecodeGenListenerHelper.getLocalVarSize(ctx) + "\n";
	}
	
	
	
	@Override
	public void exitVar_decl(MiniCParser.Var_declContext ctx) {
		String varName = ctx.IDENT().getText();
		String varDecl = "";
		
		if (BytecodeGenListenerHelper.isDeclWithInit(ctx)) {
			varDecl += "putfield " + varName + "\n";  
			// v. initialization => Later! skip now..: 
		}
		newTexts.put(ctx, varDecl);
	}
	
	
	@Override
	public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
		String varDecl = "";
		
		if (BytecodeGenListenerHelper.isDeclWithInit(ctx)) {
			String vId = symbolTable.getVarId(ctx);
			varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
					+ "istore_" + vId + "\n"; 			
		}
		else if(BytecodeGenListenerHelper.isArrayDecl(ctx)){
			String ArrayId = symbolTable.getVarId(ctx);
			varDecl += "bipush " + ctx.LITERAL().getText() + "\n"
					+ "newarray" + "	int" + "\n"
					+ "astore_" + ArrayId +"\n";
		}
		newTexts.put(ctx, varDecl);
	}

	
	// compound_stmt	: '{' local_decl* stmt* '}'
	@Override
	public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {

		String local = "";
		String stmt = "";
		for(int i = 0; i < ctx.local_decl().size() ; i++){
			local+= newTexts.get(ctx.local_decl(i));
		}

		for(int i = 0; i< ctx.stmt().size() ; i++){
			stmt += newTexts.get(ctx.stmt(i));
		}

		if(local == "" && stmt == ""){ return; }
		else if(local == "" && stmt != ""){
			newTexts.put(ctx,stmt);
		}
		else if(local != "" && stmt == ""){
			newTexts.put(ctx,local);
		}
		else{
			newTexts.put(ctx,local+stmt);
		}
		// <(3) Fill here>
		/* local들과 stmt로 이루어 졌기 때문에 .local_decl, .stmt을 get을 통해 받아준뒤 local,stmt의 존재여부
		* 에 따라 만들어진 String 을 put한다. */
	}

	// if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
	@Override
	public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
		String stmt = "";
		String condExpr= newTexts.get(ctx.expr());
		String thenStmt = newTexts.get(ctx.stmt(0));
		
		String lend = symbolTable.newLabel();
		String lelse = symbolTable.newLabel();

		if(BytecodeGenListenerHelper.noElse(ctx)) {
			stmt += condExpr + "\n"
				+ "ifeq " + lend + "\n"
				+ thenStmt + "\n"
				+ lend + ":"  + "\n";	
		}
		else {
			String elseStmt = newTexts.get(ctx.stmt(1));
			stmt += condExpr + "\n"
					+ "ifeq " + lelse + "\n"
					+ thenStmt + "\n"
					+ "goto " + lend + "\n"
					+ lelse + ": " + elseStmt + "\n"
					+ lend + ":"  + "\n";
		}
		
		newTexts.put(ctx, stmt);
	}
	
	
	// return_stmt	: RETURN ';' | RETURN expr ';'
	@Override
	public void exitReturn_stmt(Return_stmtContext ctx) {
		String reText = "";
		if(BytecodeGenListenerHelper.isIntReturn(ctx)){
			reText = "iload_"+ symbolTable.getVarId(ctx.getChild(1).getText())+"\n"+"ireturn"+"\n"+".end method"+"\n\n";
		}
		newTexts.put(ctx, reText);
			// <(4) Fill here>
	}

	
	@Override
	public void exitExpr(MiniCParser.ExprContext ctx) {
		String expr = "";

		if(ctx.getChildCount() <= 0) {
			newTexts.put(ctx, ""); 
			return;
		}		
		
		if(ctx.getChildCount() == 1) { // IDENT | LITERAL
			if(ctx.IDENT() != null) {
				String idName = ctx.IDENT().getText();
				if(symbolTable.getVarType(idName) == SymbolTable.Type.INT) {
					expr += "iload_" + symbolTable.getVarId(idName) + " \n";
				}
				else if(symbolTable.getVarType(idName) == SymbolTable.Type.INTARRAY){
					expr += "aload_"+symbolTable.getVarId(idName)+ " \n";
				}
				//else	// Type int array => Later! skip now..
				//	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
				} else if (ctx.LITERAL() != null) {
					String literalStr = ctx.LITERAL().getText();
					expr += "ldc " + literalStr + " \n";
				}
			} else if(ctx.getChildCount() == 2) { // UnaryOperation
			expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);			
		}
		else if(ctx.getChildCount() == 3) {	 
			if(ctx.getChild(0).getText().equals("(")) { 		// '(' expr ')'
				expr = newTexts.get(ctx.expr(0));
				
			} else if(ctx.getChild(1).getText().equals("=")) { 	// IDENT '=' expr
				expr = newTexts.get(ctx.expr(0))
						+ "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
				
			} else { 											// binary operation
				expr = handleBinExpr(ctx, expr);
				
			}
		}
		// IDENT '(' args ')' |  IDENT '[' expr ']'
		else if(ctx.getChildCount() == 4) {
			if(ctx.args() != null){		// function calls
				expr = handleFunCall(ctx, expr);
			} else {
				expr += "aload_"+symbolTable.getVarId(ctx.IDENT().getText())+"\n"
					+ newTexts.get(ctx.expr(0))
					+ "iaload \n";
				// expr
				// Arrays: TODO  
			}
		}
		// IDENT '[' expr ']' '=' expr
		else { // Arrays: TODO			*/
			expr += "aload_"+symbolTable.getVarId(ctx.IDENT().getText())+"\n"
					+ newTexts.get(ctx.expr(0))
					+ newTexts.get(ctx.expr(1))
					+ "iastore \n";
		}
		newTexts.put(ctx, expr);
	}


	private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
		String l1 = symbolTable.newLabel();
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr += newTexts.get(ctx.expr(0));
		switch(ctx.getChild(0).getText()) {
		case "-":
				expr += "           ineg \n"; break;
			case "--":
				expr += "ldc 1" + "\n"
						+ "isub" + "\n"
						+ "istore_" + symbolTable.getVarId(ctx.getChild(1).getText()) +"\n";
				break;
			case "++":
				expr += "ldc 1" + "\n"
						+ "iadd" + "\n"
						+ "istore_" + symbolTable.getVarId(ctx.getChild(1).getText()) + "\n";
				break;
			case "!":
				expr += "ifeq " + l2 + "\n"
						+ l1 + ": " + "ldc 0" + "\n"
					+ "goto " + lend + "\n"
					+ l2 + ": " + "ldc 1" + "\n"
					+ lend + ": " + "\n";
			break;
		}
		return expr;
	}


	private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
		String l2 = symbolTable.newLabel();
		String lend = symbolTable.newLabel();
		
		expr += newTexts.get(ctx.expr(0));
		expr += newTexts.get(ctx.expr(1));
		
		switch (ctx.getChild(1).getText()) {
			case "*":
				expr += "imul \n"; break;
			case "/":
				expr += "idiv \n"; break;
			case "%":
				expr += "irem \n"; break;
			case "+":		// expr(0) expr(1) iadd
				expr += "iadd \n"; break;
			case "-":
				expr += "isub \n"; break;
				
			case "==":
				expr += "isub " + "\n"
						+ "ifeq "+ l2 + "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;
			case "!=":
				expr += "isub " + "\n"
						+ "ifne "+ l2+ "\n"
						+ "ldc 0" + "\n"
						+ "goto " + lend + "\n"
						+ l2 + ": " + "ldc 1" + "\n"
						+ lend + ": " + "\n";
				break;
			case "<=":
				expr += "isub " + "\n"
						+ "ifle" + l2 + "\n"
						+ "ldc 0" + "\n"
						+ "goto" + lend +"\n"
						+ l2 + ": " + "ldc 1" + "\n"
						+ lend + ": " + "\n";

				// <(5) Fill here>
				/* 오른쪽 피연산자가 크다면 뻴셈 연산을 수행하였을때 0또는 음수가 나와야 한다.
				* 이 둘의 sub연산을 수행후 나오는 수를 ifle를 이용하여 0과 갇거나 작을때를 확인해 준뒤
				* 참이면 l2로 이동하고 참이라는 뜻의 1을 거짓이면 lend로 이동하여 거짓이란 뜻의 0을 가지고 수행한다.*/
				break;
			case "<":
				expr += "isub" + "\n"
						+ "iflt" + l2 +"\n"
						+ "ldc 0" + "\n"
						+ "goto"+lend+"\n"
						+ l2 + ": " + "ldc 1" + "\n"
						+ lend + ": " + "\n";
				// <(6) Fill here>
				break;
				/* 이번에는 뺄셈을 수행하였을때 음수가 나와야 하므로 iflt를 이용하여 비교해 준뒤
				* 위와 똑같이 진행한다. */

			case ">=":
				expr += "isub"+"\n"
						+ "ifge" + l2 + "\n"
						+ "ldc 0" + "\n"
						+ "goto" + lend + "\n"
						+ l2 + ": " + "ldc 1" + "\n"
						+ lend + ": " + "\n";
				// <(7) Fill here>
				/* 이번에는 양수또는 0이 나와야 왼쪽피연산자가 0과 같거나 크므로 ifge를 이용한다
				* 그 뒤는 위와 똑같이 진행한다.*/

				break;

			case ">":
				expr += "isub"+"\n"
						+ "ifgt"+ l2 + "\n"
						+ "ldc 0" +"\n"
						+ "goto" + lend + "\n"
						+ l2 + ": " + "ldc 1" + "\n"
						+ lend + ": " + "\n";

				// <(8) Fill here>
				break;
				/* 이번에는 양수만 나와야 하므로 이를 ifgt와 비교한다.
				그 뒤는 위와 똑같이 진행한다.
				 */

			case "and":
				expr +=  "ifne "+ lend + "\n"
						+ "pop" + "\n" + "ldc 0" + "\n"
						+ lend + ": " + "\n"; break;
			case "or":
				expr +=  "ifeq "+ lend + "\n"
						+ "pop" + "\n" + "ldc 1" + "\n"
						+ lend + ": " + "\n"; break;
				// <(9) Fill here>
			    /* and연산에서 피연산자의 논리값이 0이 아니면 참이라는 거니까 이에 해당하는 lend로 이동한다
			    * 거짓이라면 위 피연산자 값을 제거하고 거짓이라는 0을 넣는다.
			    * and연산은 두 연산 다 맞아야 하지만 처음부터 거짓이므로 이에대한 0을 넣는 것이라 생각된다.*/
			    /* 위 and연산처럼 한다면 eq를 이용하여 0인지 비교해서 참이면 논리값이 0이고 거짓이라는 거니까
			    그 다음 연산을 봐야되므로 lend로 이동하지만 이 연산이 0이 아니면 참이고 그 뒤의 논리형 연산 결관는
			     볼 필요가 없다. 그러므로 1을 넣는다.*/

		}
		return expr;
	}
	private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
		String fname = BytecodeGenListenerHelper.getFunName(ctx);

		if (fname.equals("_print")) {		// System.out.println	
			expr = "getstatic java/lang/System/out Ljava/io/PrintStream; "
			  		+ newTexts.get(ctx.args()) 
			  		+ "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
		} else {	
			expr = newTexts.get(ctx.args()) 
					+ "invokestatic " + BytecodeGenListenerHelper.getCurrentClassName()+ "/" + symbolTable.getFunSpecStr(fname) + "\n";
		}	
		
		return expr;
			
	}

	// args	: expr (',' expr)* | ;
	@Override
	public void exitArgs(MiniCParser.ArgsContext ctx) {

		String argsStr = "\n";
		
		for (int i=0; i < ctx.expr().size() ; i++) {
			argsStr += newTexts.get(ctx.expr(i)) ; 
		}		
		newTexts.put(ctx, argsStr);
	}

}
