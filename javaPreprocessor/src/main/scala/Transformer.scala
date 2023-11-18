package javaPreprocessor

import com.github.javaparser.ast.*
import com.github.javaparser.ast.`type`.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.GenericVisitor

import java.util.Optional

import ErrorReporter.ErrorLevel.NonFatalError

final class Transformer extends PreprocessorStage[CompilationUnit, CompilationUnit] {

  // TODO could probably be made more efficient by modifying the nodes in-place instead of creating new ones

  override protected def runImpl(input: CompilationUnit, errorReporter: ErrorReporter): Option[CompilationUnit] = {
    val output = input.accept(
      new TransformationVisitor(),
      Ctx(errorReporter, input.getStorage.map(_.getFileName).orElse("<unknown source>"))
    ).asInstanceOf[CompilationUnit]
    Some(output)
  }

  private def makeMethodName(name: String): String = s"___JumboTracer_Markers___." ++ name

  private def markExpr(expression: Expression): Expression = {
    val range = expression.getRange.get()
    new MethodCallExpr(
      makeMethodName("expr"),
      expression,
      new IntegerLiteralExpr(range.begin.line.toString),
      new IntegerLiteralExpr(range.begin.column.toString),
      new IntegerLiteralExpr(range.end.line.toString),
      new IntegerLiteralExpr(range.end.column.toString)
    )
  }

  private def ackNewStat(statement: Statement): Statement = {
    val range = statement.getRange.get()
    ackNewStat(range)
  }

  private def ackNewStat(range: com.github.javaparser.Range): Statement = {
    ackNewStat(range.begin.line, range.begin.column, range.end.line, range.end.column)
  }

  private def ackNewStat(beginLine: Int, beginCol: Int, endLine: Int, endCol: Int): Statement = {
    val call = new MethodCallExpr(
      makeMethodName("stat"),
      new IntegerLiteralExpr(beginLine.toString),
      new IntegerLiteralExpr(beginCol.toString),
      new IntegerLiteralExpr(endLine.toString),
      new IntegerLiteralExpr(endCol.toString)
    )
    new ExpressionStmt(call)
  }

  private final case class Ctx(er: ErrorReporter, filename: String)

  private final class TransformationVisitor extends GenericVisitor[Node, Ctx] {

    override def visit(n: CompilationUnit, ctx: Ctx): CompilationUnit = {
      new CompilationUnit(
        n.getTokenRange.getOrNull(),
        n.getPackageDeclaration.propagateAndCast(ctx),
        n.getImports.propagateAndCast(ctx),
        n.getTypes.propagateAndCast(ctx),
        n.getModule.propagateAndCast(ctx)
      )
    }

    override def visit(pDecl: PackageDeclaration, ctx: Ctx): PackageDeclaration = pDecl

    override def visit(tParam: TypeParameter, ctx: Ctx): TypeParameter = tParam

    override def visit(lc: LineComment, ctx: Ctx): LineComment = lc

    override def visit(bc: BlockComment, ctx: Ctx): BlockComment = bc

    override def visit(n: ClassOrInterfaceDeclaration, ctx: Ctx): ClassOrInterfaceDeclaration = {
      new ClassOrInterfaceDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.isInterface,
        n.getName.propagateAndCast(ctx),
        n.getTypeParameters.propagateAndCast(ctx),
        n.getExtendedTypes.propagateAndCast(ctx),
        n.getImplementedTypes.propagateAndCast(ctx),
        n.getPermittedTypes.propagateAndCast(ctx),
        n.getMembers.propagateAndCast(ctx)
      )
    }

    override def visit(n: RecordDeclaration, ctx: Ctx): RecordDeclaration = {
      new RecordDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.getParameters.propagateAndCast(ctx),
        n.getTypeParameters.propagateAndCast(ctx),
        n.getImplementedTypes.propagateAndCast(ctx),
        n.getMembers.propagateAndCast(ctx),
        n.getReceiverParameter.propagateAndCast(ctx)
      )
    }

    override def visit(n: CompactConstructorDeclaration, ctx: Ctx): CompactConstructorDeclaration = {
      new CompactConstructorDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getTypeParameters.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.getThrownExceptions.propagateAndCast(ctx),
        n.getBody.propagateAndCast(ctx)
      )
    }

    override def visit(n: EnumDeclaration, ctx: Ctx): EnumDeclaration = {
      new EnumDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.getImplementedTypes.propagateAndCast(ctx),
        n.getEntries.propagateAndCast(ctx),
        n.getMembers.propagateAndCast(ctx)
      )
    }

    override def visit(n: EnumConstantDeclaration, ctx: Ctx): EnumConstantDeclaration = {
      new EnumConstantDeclaration(
        n.getTokenRange.getOrNull(),
        n.getAnnotations.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.getArguments.propagateAndCast(ctx),
        n.getClassBody.propagateAndCast(ctx)
      )
    }

    override def visit(n: AnnotationDeclaration, ctx: Ctx): AnnotationDeclaration = {
      new AnnotationDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.stream().map(visit(_, ctx)).toNodeList(),
        n.getAnnotations.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.getMembers.propagateAndCast(ctx)
      )
    }

    override def visit(n: AnnotationMemberDeclaration, ctx: Ctx): AnnotationMemberDeclaration = {
      new AnnotationMemberDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getType,
        n.getName.propagateAndCast(ctx),
        n.getDefaultValue.propagateAndCast(ctx)
      )
    }

    override def visit(n: FieldDeclaration, ctx: Ctx): FieldDeclaration = {
      new FieldDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getVariables.propagateAndCast(ctx)
      )
    }

    override def visit(n: VariableDeclarator, ctx: Ctx): VariableDeclarator = {
      new VariableDeclarator(
        n.getTokenRange.getOrNull(),
        n.getType,
        n.getName.propagateAndCast(ctx),
        n.getInitializer.propagateAndCast(ctx)
      )
    }

    override def visit(n: ConstructorDeclaration, ctx: Ctx): ConstructorDeclaration = {
      new ConstructorDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getTypeParameters.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.getParameters.propagateAndCast(ctx),
        n.getThrownExceptions.propagateAndCast(ctx),
        n.getBody.propagateAndCast(ctx),
        n.getReceiverParameter.propagateAndCast(ctx)
      )
    }

    override def visit(n: MethodDeclaration, ctx: Ctx): MethodDeclaration = {
      new MethodDeclaration(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getTypeParameters.propagateAndCast(ctx),
        n.getType,
        n.getName.propagateAndCast(ctx),
        n.getParameters.propagateAndCast(ctx),
        n.getThrownExceptions.propagateAndCast(ctx),
        n.getBody.propagateAndCast(ctx),
        n.getReceiverParameter.propagateAndCast(ctx)
      )
    }

    override def visit(n: Parameter, ctx: Ctx): Parameter = {
      new Parameter(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getType,
        n.isVarArgs,
        n.getVarArgsAnnotations.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx)
      )
    }

    override def visit(n: InitializerDeclaration, ctx: Ctx): InitializerDeclaration = {
      new InitializerDeclaration(
        n.getTokenRange.getOrNull(),
        n.isStatic,
        n.getBody.propagateAndCast(ctx)
      )
    }

    override def visit(jdocComment: JavadocComment, ctx: Ctx): JavadocComment = jdocComment

    override def visit(tpe: ClassOrInterfaceType, ctx: Ctx): ClassOrInterfaceType = tpe

    override def visit(tpe: PrimitiveType, ctx: Ctx): PrimitiveType = tpe

    override def visit(tpe: ArrayType, ctx: Ctx): ArrayType = tpe

    override def visit(n: ArrayCreationLevel, ctx: Ctx): ArrayCreationLevel = {
      new ArrayCreationLevel(
        n.getTokenRange.getOrNull(),
        n.getDimension.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx)
      )
    }

    override def visit(tpe: IntersectionType, ctx: Ctx): IntersectionType = tpe

    override def visit(tpe: UnionType, ctx: Ctx): UnionType = tpe

    override def visit(tpe: VoidType, ctx: Ctx): VoidType = tpe

    override def visit(tpe: WildcardType, ctx: Ctx): WildcardType = tpe

    override def visit(tpe: UnknownType, ctx: Ctx): UnknownType = tpe

    override def visit(n: ArrayAccessExpr, ctx: Ctx): Expression = {
      markExpr(
        new ArrayAccessExpr(
          n.getTokenRange.getOrNull(),
          n.getName.propagateAndCast(ctx),
          n.getIndex.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: ArrayCreationExpr, ctx: Ctx): Expression = {
      markExpr(
        new ArrayCreationExpr(
          n.getTokenRange.getOrNull(),
          n.getElementType,
          n.getLevels.propagateAndCast(ctx),
          n.getInitializer.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: ArrayInitializerExpr, ctx: Ctx): Expression = {
      // should not be marked
      new ArrayInitializerExpr(
        n.getTokenRange.getOrNull(),
        n.getValues.propagateAndCast(ctx)
      )
    }

    override def visit(n: AssignExpr, ctx: Ctx): Expression = {
      markExpr(
        new AssignExpr(
          n.getTokenRange.getOrNull(),
          n.getTarget.propagateAndCast(ctx),
          n.getValue.propagateAndCast(ctx),
          n.getOperator
        )
      )
    }

    override def visit(n: BinaryExpr, ctx: Ctx): Expression = {
      markExpr(
        new BinaryExpr(
          n.getTokenRange.getOrNull(),
          n.getLeft.propagateAndCast(ctx),
          n.getRight.propagateAndCast(ctx),
          n.getOperator
        )
      )
    }

    override def visit(n: CastExpr, ctx: Ctx): Expression = {
      markExpr(
        new CastExpr(
          n.getTokenRange.getOrNull(),
          n.getType,
          n.getExpression.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: ClassExpr, ctx: Ctx): Expression = {
      markExpr(
        new ClassExpr(
          n.getTokenRange.getOrNull(),
          n.getType
        )
      )
    }

    override def visit(n: ConditionalExpr, ctx: Ctx): Expression = {
      markExpr(
        new ConditionalExpr(
          n.getTokenRange.getOrNull(),
          n.getCondition.propagateAndCast(ctx),
          n.getThenExpr.propagateAndCast(ctx),
          n.getElseExpr.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: EnclosedExpr, ctx: Ctx): Expression = {
      markExpr(
        new EnclosedExpr(
          n.getTokenRange.getOrNull(),
          n.getInner.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: FieldAccessExpr, ctx: Ctx): Expression = {
      markExpr(
        new FieldAccessExpr(
          n.getTokenRange.getOrNull(),
          n.getScope.propagateAndCast(ctx),
          n.getTypeArguments.map(_.propagateAndCast(ctx)).getOrNull(),
          n.getName.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: InstanceOfExpr, ctx: Ctx): Expression = {
      markExpr(
        new InstanceOfExpr(
          n.getTokenRange.getOrNull(),
          n.getExpression.propagateAndCast(ctx),
          n.getType,
          n.getPattern.propagateAndCast(ctx)
        )
      )
    }

    override def visit(lit: StringLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(lit: IntegerLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(lit: LongLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(lit: CharLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(lit: DoubleLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(lit: BooleanLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(lit: NullLiteralExpr, ctx: Ctx): Expression = markExpr(lit)

    override def visit(n: MethodCallExpr, ctx: Ctx): Expression = {
      markExpr(
        new MethodCallExpr(
          n.getTokenRange.getOrNull(),
          n.getScope.propagateAndCast(ctx),
          n.getTypeArguments.map(_.propagateAndCast(ctx)).getOrNull(),
          n.getName.propagateAndCast(ctx),
          n.getArguments.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: NameExpr, ctx: Ctx): Expression = {
      markExpr(
        new NameExpr(
          n.getTokenRange.getOrNull(),
          n.getName.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: ObjectCreationExpr, ctx: Ctx): Expression = {
      markExpr(
        new ObjectCreationExpr(
          n.getTokenRange.getOrNull(),
          n.getScope.propagateAndCast(ctx),
          n.getType,
          n.getTypeArguments.map(_.propagateAndCast(ctx)).getOrNull(),
          n.getArguments.propagateAndCast(ctx),
          n.getAnonymousClassBody.map(_.propagateAndCast(ctx)).getOrNull()
        )
      )
    }

    override def visit(n: ThisExpr, ctx: Ctx): Expression = {
      markExpr(
        new ThisExpr(
          n.getTokenRange.getOrNull(),
          n.getTypeName.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: SuperExpr, ctx: Ctx): Expression = {
      // cannot be marked
      new SuperExpr(
        n.getTokenRange.getOrNull(),
        n.getTypeName.propagateAndCast(ctx)
      )
    }

    override def visit(n: UnaryExpr, ctx: Ctx): Expression = {
      markExpr(
        new UnaryExpr(
          n.getTokenRange.getOrNull(),
          n.getExpression.propagateAndCast(ctx),
          n.getOperator
        )
      )
    }

    override def visit(n: VariableDeclarationExpr, ctx: Ctx): Expression = {
      // cannot be marked
      new VariableDeclarationExpr(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getAnnotations.propagateAndCast(ctx),
        n.getVariables.propagateAndCast(ctx)
      )
    }

    override def visit(n: MarkerAnnotationExpr, ctx: Ctx): Expression = {
      new MarkerAnnotationExpr(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx)
      )
    }

    override def visit(n: SingleMemberAnnotationExpr, ctx: Ctx): Expression = {
      new SingleMemberAnnotationExpr(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.getMemberValue.propagateAndCast(ctx)
      )
    }

    override def visit(n: NormalAnnotationExpr, ctx: Ctx): Expression = {
      new NormalAnnotationExpr(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.getPairs.propagateAndCast(ctx)
      )
    }

    override def visit(n: MemberValuePair, ctx: Ctx): MemberValuePair = {
      new MemberValuePair(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.getValue.propagateAndCast(ctx)
      )
    }

    override def visit(n: ExplicitConstructorInvocationStmt, ctx: Ctx): Statement = {
      new ExplicitConstructorInvocationStmt(
        n.getTokenRange.getOrNull(),
        n.getTypeArguments.map(_.propagateAndCast(ctx)).getOrNull(),
        n.isThis,
        n.getExpression.propagateAndCast(ctx),
        n.getArguments.propagateAndCast(ctx)
      )
    }

    override def visit(n: LocalClassDeclarationStmt, ctx: Ctx): Statement = {
      new LocalClassDeclarationStmt(
        n.getTokenRange.getOrNull(),
        n.getClassDeclaration.propagateAndCast(ctx)
      )
    }

    override def visit(n: LocalRecordDeclarationStmt, ctx: Ctx): Statement = {
      new LocalRecordDeclarationStmt(
        n.getTokenRange.getOrNull(),
        n.getRecordDeclaration.propagateAndCast(ctx)
      )
    }

    override def visit(n: AssertStmt, ctx: Ctx): Statement = {
      new AssertStmt(
        n.getTokenRange.getOrNull(),
        n.getCheck.propagateAndCast(ctx),
        n.getMessage.propagateAndCast(ctx)
      )
    }

    override def visit(n: BlockStmt, ctx: Ctx): Statement = {
      new BlockStmt(
        n.getTokenRange.getOrNull(),
        n.getStatements.propagateAndCast(ctx).stream().flatMap { stat =>
          java.util.stream.Stream.of(ackNewStat(stat), stat)
        }.toNodeList()
      )
    }

    override def visit(n: LabeledStmt, ctx: Ctx): Statement = {
      new LabeledStmt(
        n.getTokenRange.getOrNull(),
        n.getLabel.propagateAndCast(ctx),
        n.getStatement.propagateAndCast(ctx)
      )
    }

    override def visit(emptyStmt: EmptyStmt, ctx: Ctx): Statement = emptyStmt

    override def visit(n: ExpressionStmt, ctx: Ctx): Statement = {
      new ExpressionStmt(
        n.getTokenRange.getOrNull(),
        n.getExpression.propagateAndCast(ctx)
      )
    }

    override def visit(n: SwitchStmt, ctx: Ctx): Statement = {
      new SwitchStmt(
        n.getTokenRange.getOrNull(),
        n.getSelector.propagateAndCast(ctx),
        n.getEntries.propagateAndCast(ctx)
      )
    }

    override def visit(n: SwitchEntry, ctx: Ctx): SwitchEntry = {
      new SwitchEntry(
        n.getTokenRange.getOrNull(),
        n.getLabels.propagateAndCast(ctx),
        n.getType,
        n.getStatements.propagateAndCast(ctx)
      )
    }

    override def visit(n: BreakStmt, ctx: Ctx): Statement = {
      new BreakStmt(
        n.getTokenRange.getOrNull(),
        n.getLabel.propagateAndCast(ctx)
      )
    }

    override def visit(n: ReturnStmt, ctx: Ctx): Statement = {
      new ReturnStmt(
        n.getTokenRange.getOrNull(),
        n.getExpression.propagateAndCast(ctx)
      )
    }

    override def visit(n: IfStmt, ctx: Ctx): Statement = {
      new IfStmt(
        n.getTokenRange.getOrNull(),
        n.getCondition.propagateAndCast(ctx),
        n.getThenStmt.propagateAndCast(ctx),
        n.getElseStmt.propagateAndCast(ctx)
      )
    }

    override def visit(n: WhileStmt, ctx: Ctx): Statement = {
      new WhileStmt(
        n.getTokenRange.getOrNull(),
        n.getCondition.propagateAndCast(ctx),
        n.getBody.propagateAndCast(ctx)
      )
    }

    override def visit(n: ContinueStmt, ctx: Ctx): Statement = {
      new ContinueStmt(
        n.getTokenRange.getOrNull(),
        n.getLabel.propagateAndCast(ctx)
      )
    }

    override def visit(n: DoStmt, ctx: Ctx): Statement = {
      new DoStmt(
        n.getTokenRange.getOrNull(),
        n.getBody.propagateAndCast(ctx),
        n.getCondition.propagateAndCast(ctx)
      )
    }

    override def visit(n: ForEachStmt, ctx: Ctx): Statement = {
      new ForEachStmt(
        n.getTokenRange.getOrNull(),
        n.getVariable.propagateAndCast(ctx),
        n.getIterable.propagateAndCast(ctx),
        n.getBody.propagateAndCast(ctx)
      )
    }

    override def visit(n: ForStmt, ctx: Ctx): Statement = {
      val externalStats = new NodeList[Statement]()
      n.getInitialization.forEach { initExpr =>
        externalStats.add(ackNewStat(initExpr.getRange.get()))
        externalStats.add(new ExpressionStmt(initExpr.propagateAndCast(ctx)))
      }
      val internalStats = new NodeList[Statement]()
      internalStats.addAll(makeStatsList(n.getBody.propagateAndCast(ctx)))
      n.getUpdate.forEach { updateExpr =>
        internalStats.add(ackNewStat(updateExpr.getRange.get()))
        internalStats.add(new ExpressionStmt(updateExpr.propagateAndCast(ctx)))
      }
      externalStats.add(new WhileStmt(n.getCompare.propagateAndCast(ctx), new BlockStmt(internalStats)))
      val externalBlock = new BlockStmt(externalStats)
      externalBlock.setRange(n.getRange.getOrNull())
      externalBlock
    }

    override def visit(n: ThrowStmt, ctx: Ctx): Statement = {
      new ThrowStmt(
        n.getTokenRange.getOrNull(),
        n.getExpression.propagateAndCast(ctx)
      )
    }

    override def visit(n: SynchronizedStmt, ctx: Ctx): Statement = {
      ctx.er.reportErrorPos("'synchronized' found: JumboTrace does not support concurrency",
        NonFatalError, ctx.filename, n.getRange)
      new EmptyStmt(n.getTokenRange.getOrNull())
    }

    override def visit(n: TryStmt, ctx: Ctx): Statement = {
      new TryStmt(
        n.getTokenRange.getOrNull(),
        n.getResources.propagateAndCast(ctx),
        n.getTryBlock.propagateAndCast(ctx),
        n.getCatchClauses.propagateAndCast(ctx),
        n.getFinallyBlock.propagateAndCast(ctx)
      )
    }

    override def visit(n: CatchClause, ctx: Ctx): CatchClause = {
      new CatchClause(
        n.getTokenRange.getOrNull(),
        n.getParameter.propagateAndCast(ctx),
        n.getBody.propagateAndCast(ctx)
      )
    }

    override def visit(n: LambdaExpr, ctx: Ctx): Expression = {
      markExpr(
        new LambdaExpr(
          n.getTokenRange.getOrNull(),
          n.getParameters.propagateAndCast(ctx),
          n.getBody.propagateAndCast(ctx),
          n.isEnclosingParameters
        )
      )
    }

    override def visit(n: MethodReferenceExpr, ctx: Ctx): Expression = {
      markExpr(
        new MethodReferenceExpr(
          n.getTokenRange.getOrNull(),
          n.getScope.propagateAndCast(ctx),
          n.getTypeArguments.map(_.propagateAndCast(ctx)).getOrNull(),
          n.getIdentifier
        )
      )
    }

    override def visit(n: TypeExpr, ctx: Ctx): Expression = {
      new TypeExpr(
        n.getTokenRange.getOrNull(),
        n.getType
      )
    }

    override def visit(n: NodeList[_ <: Node], ctx: Ctx): Nothing = {
      throw new UnsupportedOperationException()
    }

    override def visit(n: Name, ctx: Ctx): Name = {
      new Name(
        n.getTokenRange.getOrNull(),
        n.getQualifier.propagateAndCast(ctx),
        n.getIdentifier
      )
    }

    override def visit(sn: SimpleName, ctx: Ctx): SimpleName = sn

    override def visit(n: ImportDeclaration, ctx: Ctx): ImportDeclaration = {
      new ImportDeclaration(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.isStatic,
        n.isAsterisk
      )
    }

    override def visit(n: ModuleDeclaration, ctx: Ctx): ModuleDeclaration = {
      new ModuleDeclaration(
        n.getTokenRange.getOrNull(),
        n.getAnnotations.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx),
        n.isOpen,
        n.getDirectives.propagateAndCast(ctx)
      )
    }

    override def visit(n: ModuleRequiresDirective, ctx: Ctx): ModuleRequiresDirective = {
      new ModuleRequiresDirective(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getName.propagateAndCast(ctx)
      )
    }

    override def visit(n: ModuleExportsDirective, ctx: Ctx): ModuleExportsDirective = {
      new ModuleExportsDirective(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.getModuleNames.propagateAndCast(ctx)
      )
    }

    override def visit(n: ModuleProvidesDirective, ctx: Ctx): ModuleProvidesDirective = {
      new ModuleProvidesDirective(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.getWith.propagateAndCast(ctx)
      )
    }

    override def visit(n: ModuleUsesDirective, ctx: Ctx): ModuleUsesDirective = {
      new ModuleUsesDirective(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx)
      )
    }

    override def visit(n: ModuleOpensDirective, ctx: Ctx): ModuleOpensDirective = {
      new ModuleOpensDirective(
        n.getTokenRange.getOrNull(),
        n.getName.propagateAndCast(ctx),
        n.getModuleNames.propagateAndCast(ctx)
      )
    }

    override def visit(unparsableStmt: UnparsableStmt, ctx: Ctx): Statement = unparsableStmt

    override def visit(n: ReceiverParameter, ctx: Ctx): ReceiverParameter = {
      new ReceiverParameter(
        n.getTokenRange.getOrNull(),
        n.getAnnotations.propagateAndCast(ctx),
        n.getType,
        n.getName.propagateAndCast(ctx)
      )
    }

    override def visit(varType: VarType, ctx: Ctx): VarType = varType

    override def visit(modifier: Modifier, ctx: Ctx): Modifier = modifier

    override def visit(n: SwitchExpr, ctx: Ctx): Expression = {
      markExpr(
        new SwitchExpr(
          n.getTokenRange.getOrNull(),
          n.getSelector.propagateAndCast(ctx),
          n.getEntries.propagateAndCast(ctx)
        )
      )
    }

    override def visit(n: YieldStmt, ctx: Ctx): Statement = {
      new YieldStmt(
        n.getTokenRange.getOrNull(),
        n.getExpression.propagateAndCast(ctx)
      )
    }

    override def visit(tbLitExpr: TextBlockLiteralExpr, ctx: Ctx): Expression = markExpr(tbLitExpr)

    override def visit(n: PatternExpr, ctx: Ctx): Expression = {
      // cannot be marked
      new PatternExpr(
        n.getTokenRange.getOrNull(),
        n.getModifiers.propagateAndCast(ctx),
        n.getType,
        n.getName.propagateAndCast(ctx)
      )
    }

    private def makeBlock(stat: Statement): BlockStmt = {
      stat match
        case blockStmt: BlockStmt => blockStmt
        case _ => new BlockStmt(new NodeList[Statement](stat))
    }

    private def makeStatsList(stat: Statement): NodeList[Statement] = {
      stat match
        case blockStmt: BlockStmt => blockStmt.getStatements
        case _ => new NodeList[Statement](stat)
    }

    extension[T >: Null] (opt: Optional[T]) private def getOrNull(): T = {
      if opt.isPresent then opt.get() else null
    }

    extension[N <: Node] (str: java.util.stream.Stream[N]) private def toNodeList(): NodeList[N] = {
      val ls = new NodeList[N]()
      str.forEach(ls.add)
      ls
    }

    extension (n: BlockStmt) private def propagateAndCast(ctx: Transformer.this.Ctx): BlockStmt = {
      n.accept(this, ctx).asInstanceOf[BlockStmt]
    }

    extension[T >: Null <: Node] (n: T) private def propagateAndCast(ctx: Transformer.this.Ctx): T = {
      n.accept(this, ctx).asInstanceOf[T]
    }

    extension[N <: Node] (ls: NodeList[N]) private def propagateAndCast(ctx: Ctx): NodeList[N] = {
      ls.stream().map(_.accept(this, ctx).asInstanceOf[N]).toNodeList()
    }

    extension[N >: Null <: Node] (opt: Optional[N]) private def propagateAndCast(ctx: Ctx): N = {
      opt.map(_.accept(this, ctx).asInstanceOf[N]).getOrNull()
    }

  }

}
