package gass.parser;

import gass.io.log.Log;
import gass.io.log.LogType;
import gass.tokenizer.Token;
import gass.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Stack;

public class Parser {
    public ArrayList<Token> tokens;
    public ArrayList<Enum> enums = new ArrayList<>();
    public ArrayList<Class> classes = new ArrayList<>();
    public ArrayList<Block> blocks = new ArrayList<>();
    public Parser(final ArrayList<Token> tokens) {
        this.tokens = tokens;

        // func info
        // check   -> check code exceptions
        // rename  -> rename
        // delete  -> delete
        // add     -> add
        // declare -> declare
        // parse   -> parse code

        checkAssign(); // check (= ENDLINE)

        // :e (e [e {e ENDe
        deleteBlockEndline();
        // (BLOCK) & [BLOCK] & {BLOCK}
        declrateBlock(tokens, TokenType.CIRCLE_BLOCK_BEGIN, TokenType.CIRCLE_BLOCK_END);
        parseFigureBracket(tokens);
        parseSquareBracket(tokens);
        // : BLOCK end
        declrateBlock(tokens, TokenType.BLOCK_BEGIN, TokenType.END);

        declarateEnum();  // enum
        declarateClass(); // public/private class

        parseGlobalBlocks(); // func/proc/none global block
        tokens.clear();      // clear all tokenizer tokens
    //    checkProcedureAssign();                  // check (= PROCEDURE_ASSIGN)
        //for (final Block block : blocks)
        //    renamelobalBlockAssign(block);       // global assign to func/proc/none

        final Block mainBlock = Block.getBlock("main", blocks);
        mainBlock.parseBlock(blocks);
        //System.out.println("# "+mainBlock.name);
        //declarateVariable(mainBlock); // variables <- in global and local blocks
        //declarateResult(mainBlock);   // return    <- in global and local block
    }
    /** get error line tokens output */
    private String getErrorLineOutput(final int errorToken, final ArrayList<Token> tokens) {
        final ArrayList<Token> result = new ArrayList<>();

        int lineBegin = 0;
        for (int i = errorToken-1; i > 0; i--) {
            if (tokens.get(i).type == TokenType.ENDLINE) {
                lineBegin = i;
                break;
            }
        }
        for (int i = lineBegin+1; i < tokens.size() && tokens.get(i).type != TokenType.ENDLINE; i++)
            result.add(tokens.get(i));

        return Token.tokensToString(result, false);
    }
    /** check (= ENDLINE) */
    private void checkAssign() {
        for (int i = 0; i < tokens.size(); i++) {
            final TokenType tokenType = tokens.get(i).type;
            if (Token.checkOperator(tokenType) && tokens.get(i).data == null) {
                if (i-1 >= 0 && tokenType != TokenType.NOT) // NOT no have left value (!a)
                    if (tokens.get(i-1).type == TokenType.ENDLINE) {
                        new Log(LogType.error, "Expected a left-hand value to assign it: ["+getErrorLineOutput(i,tokens)+"]");
                    }
                if (i+1 < tokens.size())
                    if (tokens.get(i+1).type == TokenType.ENDLINE) {
                        new Log(LogType.error, "Expected a right-hand value to assign it: ["+getErrorLineOutput(i,tokens)+"]");
                    }
            }
        }
        //
    }
    /** delete e (endline token) :e (e [e {e ENDe */
    private void deleteBlockEndline() {
        for (int i = 0; i+1 < tokens.size(); i++) {
            if (tokens.get(i+1).type == TokenType.ENDLINE) {
                final TokenType type = tokens.get(i).type;
                if (type == TokenType.BLOCK_BEGIN || type == TokenType.CIRCLE_BLOCK_BEGIN ||
                    type == TokenType.SQUARE_BLOCK_BEGIN || type == TokenType.FIGURE_BLOCK_BEGIN) {
                    tokens.remove(i+1);
                    i--;
                }
            }
            //
        }
    }
    /** parse [] brackets */
    private void parseSquareBracket(final ArrayList<Token> tokens) {
        for (final Token token : tokens) {
            if (token.childrens != null)
                parseSquareBracket(token.childrens);
        }
        declrateBlock(tokens, TokenType.SQUARE_BLOCK_BEGIN, TokenType.SQUARE_BLOCK_END);
    }
    /** parse {} brackets */
    private void parseFigureBracket(final ArrayList<Token> tokens) {
        for (final Token token : tokens) {
            if (token.childrens != null)
                parseFigureBracket(token.childrens);
        }
        declrateBlock(tokens, TokenType.FIGURE_BLOCK_BEGIN, TokenType.FIGURE_BLOCK_END);
    }
    /** parse block BEGIN -> END */
    private void declrateBlock(final ArrayList<Token> tokens, final TokenType beginType, final TokenType endType) {
        final Stack<Integer> blocks = new Stack<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).type == beginType) {      // begin
                blocks.push(i);
            } else if (tokens.get(i).type == endType) { // end
                final int lastBlock = blocks.size()-2;
                if (lastBlock >= 0) {
                    tokens.get( blocks.get(lastBlock) ).addChildren( tokens.get(blocks.peek()) );
                    tokens.remove( blocks.peek().intValue() );
                    i--;
                }
                blocks.pop();
                tokens.remove(i);
                i--;
            } else if (!blocks.isEmpty()) { // add new childrens to token
                tokens.get(blocks.peek()).addChildren(new Token(tokens.get(i).data, tokens.get(i).type, tokens.get(i).childrens));
                tokens.remove(i);
                i--;
            }
        }
    }
    /** parse enum block */
    private void declarateEnum() {
        for (int i = 0; i+2 < tokens.size(); i++) {
            final Token token2 = tokens.get(i+1);
            final Token token3 = tokens.get(i+2);
            if (tokens.get(i).type == TokenType.ENUM && token2.type == TokenType.WORD && token3.type == TokenType.BLOCK_BEGIN) {
                enums.add(new Enum(token2.data, token3.childrens));
                tokens.remove(i); // enum
                tokens.remove(i); // name
                tokens.remove(i); // block
                i--;
            }
        }
    }
    /** parse class block (public/private) */
    private void declarateClass() {
        for (int i = 0; i+2 < tokens.size(); i++) {
            final Token token2 = tokens.get(i+1);
            final Token token3 = tokens.get(i+2);
            if (tokens.get(i).type == TokenType.PRIVATE && token2.type == TokenType.WORD && token3.type == TokenType.BLOCK_BEGIN) {
                classes.add(new Class(token2.data, ClassType.PRIVATE, token3.childrens));
                tokens.remove(i); // private/public
                tokens.remove(i); // name
                tokens.remove(i); // block
                i--;
            } else
            if (tokens.get(i).type == TokenType.PUBLIC && token2.type == TokenType.WORD && token3.type == TokenType.BLOCK_BEGIN) {
                classes.add(new Class(token2.data, ClassType.PUBLIC, token3.childrens));
                tokens.remove(i); // private/public
                tokens.remove(i); // name
                tokens.remove(i); // block
                i--;
            }
        }
    }
    /** add new global block with parameters and check exist */
    private void addGlobalBlock(final String name, final BlockType type, final ArrayList<Token> parameters, final ArrayList<Token> tokens) {
        for (final Block b : blocks) { // check exist
            if (Objects.equals(b.name, name))
                new Log(LogType.error,"The global ["+b.name+"] block has been re-declared");
        }
        final Block block = new Block(name, type, parameters, tokens);
        declareLocalBlocks(block);
        declareLines(block);
        blocks.add(block);
    }
    /** add new global block with no parameters and check exist */
    private void addGlobalBlock(final String name, final BlockType type, final ArrayList<Token> tokens) {
        for (final Block b : blocks) { // check exist
            if (Objects.equals(b.name, name))
                new Log(LogType.error,"The global ["+b.name+"] block has been re-declared");
        }
        final Block block = new Block(name, type, tokens);
        declareLocalBlocks(block);
        declareLines(block);
        blocks.add(block);
    }
    /** parse global block func/proc/none */
    private void parseGlobalBlocks() {
        for (int i = 0; i+1 < tokens.size(); i++) {
            // type
            final BlockType type;
            if (i-1 >= 0) {
                if (tokens.get(i-1).type == TokenType.FUNCTION) {
                    type = BlockType.FUNCTION;
                    tokens.remove(i-1);
                    i--;
                } else
                if (tokens.get(i-1).type == TokenType.PROCEDURE) {
                    type = BlockType.PROCEDURE;
                    tokens.remove(i-1);
                    i--;
                } else type = BlockType.NONE;
            } else type = BlockType.NONE;


            // declaration
            final Token token2 = tokens.get(i+1);
            if (tokens.get(i).type == TokenType.WORD && token2.type == TokenType.CIRCLE_BLOCK_BEGIN && i+2 < tokens.size()) {
                final Token token3 = tokens.get(i+2);
                if (token3.type == TokenType.BLOCK_BEGIN) {
                    // block with parameters
                    addGlobalBlock(tokens.get(i).data, type, token2.childrens, token3.childrens);
                    tokens.remove(i); // name
                    tokens.remove(i); // parameters
                    tokens.remove(i); // block
                    i--;
                }
            } else
            if (tokens.get(i).type == TokenType.WORD && token2.type == TokenType.BLOCK_BEGIN) {
                // block with no parameters
                addGlobalBlock(tokens.get(i).data, type, token2.childrens);
                tokens.remove(i); // name
                tokens.remove(i); // block
                i--;
            }
        }
        //
    }
    /** cycle parse local block proc/func/none  */
    private void declareLocalBlocks(final Block block) {
        if (block.lines == null) return; // if no tokens in global block => no local blocks
        final ArrayList<Token> firstLine = block.lines.get(0);
        if (firstLine == null || firstLine.isEmpty()) return;

        int assignNum = 0;
        for (int i = 0; i < firstLine.size(); i++) {
            if (firstLine.get(i).type == TokenType.BLOCK_BEGIN) {
                final BlockType newBlockType;
                if (firstLine.get(i-1).type == TokenType.PROCEDURE)
                    newBlockType = BlockType.PROCEDURE;
                else
                if (firstLine.get(i-1).type == TokenType.FUNCTION)
                    newBlockType = BlockType.FUNCTION;
                else
                    newBlockType = BlockType.NONE;

                final String localBlockName = block.name+':'+assignNum;
                final Block newBlock = new Block(localBlockName, newBlockType, firstLine.get(i).childrens);

                if (newBlockType == BlockType.NONE) {
                    firstLine.get(i).type = TokenType.BLOCK_ASSIGN;
                    firstLine.get(i).data = localBlockName;
                    firstLine.get(i).childrens = null;
                } else {
                    if (newBlockType == BlockType.PROCEDURE)
                        firstLine.get(i).type = TokenType.PROCEDURE_ASSIGN;
                    else
                        firstLine.get(i).type = TokenType.FUNCTION_ASSIGN;
                    firstLine.remove(i-1);
                    i--;

                    firstLine.get(i).data = localBlockName;
                    firstLine.get(i).childrens = null;
                }
                assignNum++;

                declareLocalBlocks(newBlock);
                declareLines(newBlock);
                block.addLocalBlock(newBlock);
            }
        }
        //
    }
    /** declare block lines */
    private void declareLines(final Block block) {
        final ArrayList<Token> buffer = new ArrayList<>();
        for (final Token token : block.lines.get(0)) {
            if (token.type == TokenType.ENDLINE) {
                block.lines.add(new ArrayList<>(buffer));
                buffer.clear();
            } else buffer.add(token);
        }
        block.lines.remove(0); // delete first line
    }
    /** check (a = PROCEDURE_ASSIGN) */
    /*
    private void checkProcedureAssign() {
        for (final Block block : blocks) {
            final ArrayList<Token> firstLine = block.lines.get(0);
            if (firstLine == null || firstLine.isEmpty()) continue;

            for (int i = 1; i < firstLine.size(); i++) {
                if (firstLine.get(i).type == TokenType.PROCEDURE_ASSIGN && firstLine.get(i-1).type == TokenType.EQUAL)
                    new Log(LogType.error,"The result from the procedure in the block ["+block.name+"] is expected ["+getErrorLineOutput(i, tokens)+"]");
            }
        }
        //
    }
     */
    /** parse global assign to func/proc/none */
    /*
    private void renamelobalBlockAssign(final Block block) {
        final ArrayList<Token> firstLine = block.lines.get(0);
        if (firstLine == null || firstLine.isEmpty()) return;

        for (int i = 0; i+1 < tokens.size(); i++) {
            final Token currentToken = tokens.get(i);
            if (currentToken.type == TokenType.WORD && tokens.get(i+1).type == TokenType.CIRCLE_BLOCK_BEGIN)
                currentToken.type = TokenType.BLOCK_ASSIGN;
        }
        //
        if (block.localBlocks != null) {
            for (final Block localBlock : block.localBlocks)
                renamelobalBlockAssign(localBlock);
        }
    }
     */
    /** pre-parse block */
    /*
    private void preParseBlock(final String blockName, Block block) {
        final String[] localBlockName = blockName.split(":");
        final Block localBlock;
        if (localBlockName.length == 1)
            localBlock = Block.getBlock(blockName, blocks);
        else
            localBlock = block.localBlocks.get( Integer.parseInt(localBlockName[localBlockName.length-1]) );
        declarateVariable(localBlock);
        declarateResult(localBlock);
        parseBlock(localBlock);
    }
     */
    /** parse block */
    /*
    private void parseBlock(final Block block) {
        if (block == null) return;

        // parse block
        parseVariable(block);
        if (block.result != null) {
            block.result.setValue(block, blocks);
        }
    }
     */
    /** parse dependency block */
    /*
    private void parseDependencyBlock(final int i, final ArrayList<Token> tokens, final Block block) {
        final Token token = tokens.get(i);
        if (List.of(TokenType.BLOCK_ASSIGN, TokenType.FUNCTION_ASSIGN, TokenType.PROCEDURE_ASSIGN).contains(token.type) && i+1 < tokens.size()) {
            final Token nextToken = tokens.get(i+1);
            if (nextToken.type == TokenType.CIRCLE_BLOCK_BEGIN) {
                System.out.println(Token.tokensToString(nextToken.childrens, false));
                final ArrayList<ArrayList<Token>> parameters = Token.separateTokens(TokenType.COMMA, nextToken.childrens);

                final Block dependencyBlock = Block.getBlock(token.data, blocks);
                for (int j = 0; j < parameters.size(); j++) {
                    final ArrayList<Token> parameter = parameters.get(j);
                    if (parameter == null || parameter.isEmpty()) continue;
                    System.out.println("- "+Token.tokensToString(parameter, false));
                    //renameVariable(parameter, block);
                    for (Token t : parameter) {
                        System.out.println("\t- "+t.type);
                    }
                    dependencyBlock.parameters.get(j).value = new Expression( parameters.get(j) );
                    dependencyBlock.parameters.get(j).setValue(block, blocks);
                }
                preParseBlock(token.data, block);
            }
        }
    }
     */
    /*
    private void declarateVariable(final Block block) {
        final ArrayList<Token> tokens = block.tokens;
        if (tokens == null || tokens.isEmpty()) return;

        System.out.println("\tdeclarateVariable: "+block.name);

        for (int i = 0; i+1 < tokens.size(); i++) {
            final Token currentToken = tokens.get(i);
            if (currentToken.type == TokenType.WORD) {
                currentToken.type = TokenType.VARIABLE_NAME;
                // TO:DO: check left public/private or const/final

                // add new variable
                final ArrayList<Token> variableValue = new ArrayList<>();
                if (tokens.get(i+1).type == TokenType.EQUAL && tokens.get(i+2).type != TokenType.ENDLINE) {
                    tokens.remove(i); // remove variable name
                    tokens.remove(i); // remove =

                    // next read right tokens
                    while (i < tokens.size()) {
                        final Token nextToken = tokens.get(i);
                        if (nextToken.type == TokenType.ENDLINE) {
                            tokens.remove(i);
                            i--;
                            break;
                        } else {
                            // checkVariable
                            variableValue.add(nextToken);
                            tokens.remove(i);
                        }
                    }
                    renameVariable(variableValue, block);
                    block.addVariable(currentToken.data, variableValue);
                    block.variables.get(block.variables.size()-1).setValue(block, blocks);
                }
                //
            } else
                // link to dependency block code
                parseDependencyBlock(i, tokens, block);
        }
    }
     */
    /** declarete return */
    /*
    private void declarateResult(final Block block) {
        if (block.result != null) return;

        System.out.println("\tdeclarateResult: "+block.name);
        final ArrayList<Token> tokens = block.tokens;
        if (tokens != null && !tokens.isEmpty()) {
            for (int i = 0; i < tokens.size(); i++) {
                final Token currnetToken = tokens.get(i);
                if (currnetToken.type == TokenType.RETURN_VALUE) {
                    tokens.remove(i); // remove return

                    // add result
                    ArrayList<Token> resultValue = new ArrayList<>();
                    // next read right tokens
                    // TO:DO: in this parse assigns
                    while (i < tokens.size()) {
                        final Token nextToken = tokens.get(i);
                        if (nextToken.type == TokenType.ENDLINE) {
                            tokens.remove(i);
                            i--;
                            break;
                        } else {
                            resultValue.add(nextToken);
                            tokens.remove(i);
                        }
                    }
                    renameVariable(resultValue, block);
                    final BlockResult result = new BlockResult(resultValue);
                    result.setValue(block, blocks);
                    block.result = result;
                }
            }
            //
        }
        // local blocks
        if (block.localBlocks != null) {
            for (final Block localBlock : block.localBlocks)
                preParseBlock(localBlock.name, block);
        }
    }
     */
    /** parse variables */
    /*
    private void parseVariable(final Block block) {
        final ArrayList<Variable> variables = block.variables;
        if (variables != null && !variables.isEmpty())
            for (final Variable variable : variables)
                variable.setValue(block, blocks);
    }
     */
}
