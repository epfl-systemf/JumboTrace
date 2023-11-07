package s2sCompiler

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.`type`.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.{BlockComment, JavadocComment, LineComment}
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.*
import com.github.javaparser.ast.*
import com.github.javaparser.ast.Node.PostOrderIterator
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.{CombinedTypeSolver, JavaParserTypeSolver, ReflectionTypeSolver}
import injectionAutomation.InjectedMethods
import s2sCompiler.ErrorReporter.ErrorLevel
import s2sCompiler.ErrorReporter.ErrorLevel.*

import java.util.Optional
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

final class Transformer extends CompilerStage[(CompilationUnit, Map[Expression, Type]), CompilationUnit] {

  override protected def runImpl(input: (CompilationUnit, Map[Expression, Type]), errorReporter: ErrorReporter): Option[CompilationUnit] = {
    val (cu, typesMap) = input
    val output = cu.accept(
      new TransformationVisitor(),
      Ctx(
        errorReporter,
        cu.getStorage.map(_.getFileName).orElse("<unknown source>"),
        ListBuffer.empty,
        new FreshNamesGenerator(),
        typesMap,
        currentImplicitLabel = None
      )
    ).asInstanceOf[CompilationUnit]
    Some(output)
  }

  private final case class Ctx(
                                er: ErrorReporter,
                                filename: String,
                                extraVariables: ListBuffer[VariableDeclarator],
                                freshNamesGenerator: FreshNamesGenerator,
                                typesMap: Map[Expression, Type],
                                currentImplicitLabel: Option[String]
                              )

  private final class TransformationVisitor extends GenericVisitor[Node, Ctx] {

    private def makeBlock(stats: Seq[Statement | Expression]): BlockStmt = {
      val block = new BlockStmt()
      stats.foreach {
        {
          case stat: Statement =>
            block.addStatement(stat)
          case expr: Expression =>
            block.addStatement(new ExpressionStmt(expr))
        }
      }
      block
    }

    override def visit(n: CompilationUnit, ctx: Ctx): CompilationUnit = {
      val packageDeclaration = n.getPackageDeclaration.propagateAndCast(ctx)
      val importDeclarations = n.getImports.acceptThis(ctx)
      val typeDeclarations = n.getTypes.acceptThis(ctx)
      val moduleDeclaration = n.getModule.propagateAndCast(ctx)
      n.setPackageDeclaration(packageDeclaration)
      n.setImports(importDeclarations)
      n.setTypes(typeDeclarations)
      n.setModule(moduleDeclaration)
    }

    override def visit(pDecl: PackageDeclaration, ctx: Ctx): PackageDeclaration = pDecl

    override def visit(tParam: TypeParameter, ctx: Ctx): TypeParameter = tParam

    override def visit(lc: LineComment, ctx: Ctx): LineComment = lc

    override def visit(bc: BlockComment, ctx: Ctx): BlockComment = bc

    override def visit(n: ClassOrInterfaceDeclaration, ctx: Ctx): ClassOrInterfaceDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val typeParameters = n.getTypeParameters.acceptThis(ctx)
      val extendedTypes = n.getExtendedTypes.acceptThis(ctx)
      val implementedTypes = n.getImplementedTypes.acceptThis(ctx)
      val permittedTypes = n.getPermittedTypes.acceptThis(ctx)
      val members = n.getMembers.acceptThis(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setName(name)
      n.setTypeParameters(typeParameters)
      n.setExtendedTypes(extendedTypes)
      n.setImplementedTypes(implementedTypes)
      n.setPermittedTypes(permittedTypes)
      n.setMembers(members)
    }

    override def visit(n: RecordDeclaration, ctx: Ctx): RecordDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val parameters = n.getParameters.acceptThis(ctx)
      val typeParameters = n.getTypeParameters.acceptThis(ctx)
      val implementedTypes = n.getImplementedTypes.acceptThis(ctx)
      val members = n.getMembers.acceptThis(ctx)
      val receiverParameter = n.getReceiverParameter.propagateAndCast(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setName(name)
      n.setParameters(parameters)
      n.setTypeParameters(typeParameters)
      n.setImplementedTypes(implementedTypes)
      n.setMembers(members)
      n.setReceiverParameter(receiverParameter)
    }

    override def visit(n: CompactConstructorDeclaration, ctx: Ctx): CompactConstructorDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val typeParameters = n.getTypeParameters.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val thrownExceptions = n.getThrownExceptions.acceptThis(ctx)
      val body = n.getBody.propagateAndCast(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setTypeParameters(typeParameters)
      n.setName(name)
      n.setThrownExceptions(thrownExceptions)
      n.setBody(body)
    }

    override def visit(n: EnumDeclaration, ctx: Ctx): EnumDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val implementedTypes = n.getImplementedTypes.acceptThis(ctx)
      val entries = n.getEntries.acceptThis(ctx)
      val members = n.getMembers.acceptThis(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setName(name)
      n.setImplementedTypes(implementedTypes)
      n.setEntries(entries)
      n.setMembers(members)
    }

    override def visit(n: EnumConstantDeclaration, ctx: Ctx): EnumConstantDeclaration = {
      val annotations = n.getAnnotations.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val arguments = n.getArguments.acceptThis(ctx)
      val body = n.getClassBody.acceptThis(ctx)
      n.setAnnotations(annotations)
      n.setName(name)
      n.setArguments(arguments)
      n.setClassBody(body)
    }

    override def visit(n: AnnotationDeclaration, ctx: Ctx): AnnotationDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val members = n.getMembers.acceptThis(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setName(name)
      n.setMembers(members)
    }

    override def visit(n: AnnotationMemberDeclaration, ctx: Ctx): AnnotationMemberDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val tpe = n.getType.propagateAndCast(ctx)
      val name = visit(n.getName, ctx)
      val defaultValue = n.getDefaultValue.propagateAndCast(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setType(tpe)
      n.setName(name)
      n.setDefaultValue(defaultValue)
    }

    override def visit(n: FieldDeclaration, ctx: Ctx): FieldDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val variables = n.getVariables.acceptThis(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setVariables(variables)
    }

    override def visit(n: VariableDeclarator, ctx: Ctx): VariableDeclarator = {
      val tpe = n.getType.propagateAndCast(ctx)
      val name = visit(n.getName, ctx)
      val initializer = n.getInitializer.propagateAndCast(ctx)
      n.setType(tpe)
      n.setName(name)
      n.setInitializer(initializer)
    }

    override def visit(n: ConstructorDeclaration, ctx: Ctx): ConstructorDeclaration = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val typeParameters = n.getTypeParameters.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      val parameters = n.getParameters.acceptThis(ctx)
      val thrownExceptions = n.getThrownExceptions.acceptThis(ctx)
      val body = n.getBody.propagateAndCast(ctx)
      val receiverParameter = n.getReceiverParameter.propagateAndCast(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setTypeParameters(typeParameters)
      n.setName(name)
      n.setParameters(parameters)
      n.setThrownExceptions(thrownExceptions)
      n.setBody(body)
      n.setReceiverParameter(receiverParameter)
    }

    override def visit(n: MethodDeclaration, externalCtx: Ctx): MethodDeclaration = {
      val variables = ListBuffer.empty[VariableDeclarator]

      val modifiers = n.getModifiers.acceptThis(externalCtx)
      val annotations = n.getAnnotations.acceptThis(externalCtx)
      val typeParameters = n.getTypeParameters.acceptThis(externalCtx)
      val tpe = n.getType.propagateAndCast(externalCtx)
      val name = visit(n.getName, externalCtx)
      val parameters = n.getParameters.acceptThis(externalCtx)
      val thrownExceptions = n.getThrownExceptions.acceptThis(externalCtx)
      val body = n.getBody.propagateAndCast(
        Ctx(externalCtx.er, externalCtx.filename, variables, new FreshNamesGenerator(), externalCtx.typesMap, currentImplicitLabel = None))
      for (varDecl <- variables.reverseIterator) { // reversing just for convenience, it would work without too
        body.getStatements.addFirst(new ExpressionStmt(new VariableDeclarationExpr(varDecl)))
      }
      val receiverParameter = n.getReceiverParameter.propagateAndCast(externalCtx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setTypeParameters(typeParameters)
      n.setType(tpe)
      n.setName(name)
      n.setParameters(parameters)
      n.setThrownExceptions(thrownExceptions)
      n.setBody(body)
      n.setReceiverParameter(receiverParameter)
    }

    override def visit(n: Parameter, ctx: Ctx): Parameter = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val tpe = n.getType.propagateAndCast(ctx)
      val varArgsAnnotations = n.getVarArgsAnnotations.acceptThis(ctx)
      val name = visit(n.getName, ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setType(tpe)
      n.setVarArgsAnnotations(varArgsAnnotations)
      n.setName(name)
    }

    override def visit(n: InitializerDeclaration, ctx: Ctx): InitializerDeclaration = {
      val body = n.getBody.propagateAndCast(ctx)
      n.setBody(body)
    }

    override def visit(jdocComment: JavadocComment, ctx: Ctx): JavadocComment = jdocComment

    override def visit(tpe: ClassOrInterfaceType, ctx: Ctx): ClassOrInterfaceType = tpe

    override def visit(tpe: PrimitiveType, ctx: Ctx): PrimitiveType = tpe

    override def visit(tpe: ArrayType, ctx: Ctx): ArrayType = tpe

    override def visit(n: ArrayCreationLevel, ctx: Ctx): ArrayCreationLevel = {
      val dimension = n.getDimension.propagateAndCast(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      n.setDimension(dimension)
      n.setAnnotations(annotations)
    }

    override def visit(tpe: IntersectionType, ctx: Ctx): IntersectionType = tpe

    override def visit(tpe: UnionType, ctx: Ctx): UnionType = tpe

    override def visit(tpe: VoidType, ctx: Ctx): VoidType = tpe

    override def visit(tpe: WildcardType, ctx: Ctx): WildcardType = tpe

    override def visit(tpe: UnknownType, ctx: Ctx): UnknownType = tpe

    override def visit(arrayAccessExpr: ArrayAccessExpr, ctx: Ctx): Expression = {
      import ctx.{freshNamesGenerator, extraVariables, er, filename}
      val arrayVarId = freshNamesGenerator.nextName("array")
      val idxVarId = freshNamesGenerator.nextName("index")
      // TODO calculate types instead of using Object everywhere and handle failure in calculateResolvedType
      val arrayType = typeOf(arrayAccessExpr.getName, ctx)
      val indexType = typeOf(arrayAccessExpr.getIndex, ctx)
      extraVariables.addOne(new VariableDeclarator(arrayType, arrayVarId))
      extraVariables.addOne(new VariableDeclarator(indexType, idxVarId))
      val name = arrayAccessExpr.getName.propagateAndCast(ctx)
      val index = arrayAccessExpr.getIndex.propagateAndCast(ctx)
      arrayAccessExpr.setName(new EnclosedExpr(new AssignExpr(new NameExpr(arrayVarId), name, AssignExpr.Operator.ASSIGN)))
      arrayAccessExpr.setIndex(new AssignExpr(new NameExpr(idxVarId), index, AssignExpr.Operator.ASSIGN))
      InjectedMethods.iArrayAccess(arrayAccessExpr, arrayVarId, idxVarId)
    }

    override def visit(n: ArrayCreationExpr, ctx: Ctx): Expression = {
      val elemType = n.getElementType.propagateAndCast(ctx)
      val levels = n.getLevels.acceptThis(ctx)
      val initializerExpr = n.getInitializer.propagateAndCast(ctx)
      n.setElementType(elemType)
      n.setLevels(levels)
      n.setInitializer(initializerExpr)
    }

    override def visit(n: ArrayInitializerExpr, ctx: Ctx): Expression = {
      val values = n.getValues.acceptThis(ctx)
      n.setValues(values)
    }

    override def visit(n: AssignExpr, ctx: Ctx): Expression = {
      val target = n.getTarget.propagateAndCast(ctx)
      val value = n.getValue.propagateAndCast(ctx)
      n.setTarget(target)
      n.setValue(value)
    }

    override def visit(n: BinaryExpr, ctx: Ctx): Expression = {
      val left = n.getLeft.propagateAndCast(ctx)
      val right = n.getRight.propagateAndCast(ctx)
      n.setLeft(left)
      n.setRight(right)
    }

    override def visit(n: CastExpr, ctx: Ctx): Expression = {
      val tpe = n.getType.propagateAndCast(ctx)
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setType(tpe)
      n.setExpression(expr)
    }

    override def visit(n: ClassExpr, ctx: Ctx): Expression = {
      val tpe = n.getType.propagateAndCast(ctx)
      n.setType(tpe)
    }

    override def visit(n: ConditionalExpr, ctx: Ctx): Expression = {
      val condition = n.getCondition.propagateAndCast(ctx)
      val thenExpr = n.getThenExpr.propagateAndCast(ctx)
      val elseExpr = n.getElseExpr.propagateAndCast(ctx)
      n.setCondition(condition)
      n.setThenExpr(thenExpr)
      n.setElseExpr(elseExpr)
    }

    override def visit(n: EnclosedExpr, ctx: Ctx): Expression = {
      val inner = n.getInner.propagateAndCast(ctx)
      n.setInner(inner)
    }

    override def visit(n: FieldAccessExpr, ctx: Ctx): Expression = {
      val scope = n.getScope.propagateAndCast(ctx)
      val typeArgs = n.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      val name = visit(n.getName, ctx)
      n.setScope(scope)
      n.setTypeArguments(typeArgs)
      n.setName(name)
    }

    override def visit(n: InstanceOfExpr, ctx: Ctx): Expression = {
      val expr = n.getExpression.propagateAndCast(ctx)
      val tpe = n.getType.propagateAndCast(ctx)
      val pattern = n.getPattern.propagateAndCast(ctx)
      n.setExpression(expr)
      n.setType(tpe)
      n.setPattern(pattern)
    }

    override def visit(lit: StringLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(lit: IntegerLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(lit: LongLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(lit: CharLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(lit: DoubleLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(lit: BooleanLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(lit: NullLiteralExpr, ctx: Ctx): Expression = lit

    override def visit(n: MethodCallExpr, ctx: Ctx): Expression = {
      val scope = n.getScope.propagateAndCast(ctx)
      val typeArgs = n.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      val name = visit(n.getName, ctx)
      val args = n.getArguments.acceptThis(ctx)
      n.setScope(scope)
      n.setTypeArguments(typeArgs)
      n.setName(name)
      n.setArguments(args)
    }

    override def visit(n: NameExpr, ctx: Ctx): Expression = {
      val name = visit(n.getName, ctx)
      n.setName(name)
    }

    override def visit(n: ObjectCreationExpr, ctx: Ctx): Expression = {
      val scope = n.getScope.propagateAndCast(ctx)
      val tpe = n.getType.propagateAndCast(ctx)
      val typeArgs = n.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      val args = n.getArguments.acceptThis(ctx)
      val anonymousClassBody = n.getAnonymousClassBody.map(_.acceptThis(ctx)).getOrNull()
      n.setScope(scope)
      n.setType(tpe)
      n.setTypeArguments(typeArgs)
      n.setArguments(args)
      n.setAnonymousClassBody(anonymousClassBody)
    }

    override def visit(n: ThisExpr, ctx: Ctx): Expression = {
      val typeName = n.getTypeName.propagateAndCast(ctx)
      n.setTypeName(typeName)
    }

    override def visit(n: SuperExpr, ctx: Ctx): Expression = {
      val typeName = n.getTypeName.propagateAndCast(ctx)
      n.setTypeName(typeName)
    }

    override def visit(n: UnaryExpr, ctx: Ctx): Expression = {
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setExpression(expr)
    }

    override def visit(n: VariableDeclarationExpr, ctx: Ctx): Expression = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val annotations = n.getAnnotations.acceptThis(ctx)
      val variables = n.getVariables.acceptThis(ctx)
      n.setModifiers(modifiers)
      n.setAnnotations(annotations)
      n.setVariables(variables)
    }

    override def visit(n: MarkerAnnotationExpr, ctx: Ctx): Expression = {
      val name = n.getName.propagateAndCast(ctx)
      n.setName(name)
    }

    override def visit(n: SingleMemberAnnotationExpr, ctx: Ctx): Expression = {
      val name = n.getName.propagateAndCast(ctx)
      val memberValue = n.getMemberValue.propagateAndCast(ctx)
      n.setName(name)
      n.setMemberValue(memberValue)
    }

    override def visit(n: NormalAnnotationExpr, ctx: Ctx): Expression = {
      val name = n.getName.propagateAndCast(ctx)
      val pairs = n.getPairs.acceptThis(ctx)
      n.setName(name)
      n.setPairs(pairs)
    }

    override def visit(n: MemberValuePair, ctx: Ctx): MemberValuePair = {
      val name = n.getName.propagateAndCast(ctx)
      val value = n.getValue.propagateAndCast(ctx)
      n.setName(name)
      n.setValue(value)
    }

    override def visit(n: ExplicitConstructorInvocationStmt, ctx: Ctx): Statement = {
      val typeArgs = n.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      val expr = n.getExpression.propagateAndCast(ctx)
      val args = n.getArguments.acceptThis(ctx)
      n.setTypeArguments(typeArgs)
      n.setExpression(expr)
      n.setArguments(args)
    }

    override def visit(n: LocalClassDeclarationStmt, ctx: Ctx): Statement = {
      val classDecl = n.getClassDeclaration.propagateAndCast(ctx)
      n.setClassDeclaration(classDecl)
    }

    override def visit(n: LocalRecordDeclarationStmt, ctx: Ctx): Statement = {
      val recordDeclaration = n.getRecordDeclaration.propagateAndCast(ctx)
      n.setRecordDeclaration(recordDeclaration)
    }

    override def visit(n: AssertStmt, ctx: Ctx): Statement = {
      val check = n.getCheck.propagateAndCast(ctx)
      val msg = n.getMessage.propagateAndCast(ctx)
      n.setCheck(check)
      n.setMessage(msg)
    }

    override def visit(n: BlockStmt, ctx: Ctx): Statement = {
      val stats = n.getStatements.acceptThis(ctx)
      n.setStatements(stats)
    }

    override def visit(labeledStmt: LabeledStmt, ctx: Ctx): Statement = {
      val label = labeledStmt.getLabel.propagateAndCast(ctx)
      val stat = labeledStmt.getStatement.propagateAndCast(ctx.copy(currentImplicitLabel = Some(labeledStmt.getLabel.getIdentifier)))
      labeledStmt.setLabel(label)
      labeledStmt.setStatement(stat)
    }

    override def visit(emptyStmt: EmptyStmt, ctx: Ctx): Statement = emptyStmt

    override def visit(n: ExpressionStmt, ctx: Ctx): Statement = {
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setExpression(expr)
    }

    override def visit(switchStmt: SwitchStmt, ctx: Ctx): Statement = {
      val labelName = ctx.freshNamesGenerator.nextName("switch")
      val selector = switchStmt.getSelector.propagateAndCast(ctx)
      val entries = switchStmt.getEntries.acceptThis(ctx.copy(currentImplicitLabel = Some(labelName)))
      switchStmt.setSelector(selector)
      switchStmt.setEntries(entries)
      new LabeledStmt(labelName, switchStmt)
    }

    override def visit(n: SwitchEntry, ctx: Ctx): SwitchEntry = {
      val labels = n.getLabels.acceptThis(ctx)
      val statements = n.getStatements.acceptThis(ctx)
      n.setLabels(labels)
      n.setStatements(statements)
    }

    override def visit(n: BreakStmt, ctx: Ctx): Statement = {
      val label = n.getLabel.propagateAndCast(ctx)
      n.setLabel(label)
    }

    override def visit(n: ReturnStmt, ctx: Ctx): Statement = {
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setExpression(expr)
    }

    override def visit(n: IfStmt, ctx: Ctx): Statement = {
      val condition = n.getCondition.propagateAndCast(ctx)
      val thenStat = n.getThenStmt.propagateAndCast(ctx)
      val elseStat = n.getElseStmt.propagateAndCast(ctx)
      n.setCondition(condition)
      n.setThenStmt(thenStat)
      n.setElseStmt(elseStat)
    }

    override def visit(whileStmt: WhileStmt, ctx: Ctx): Statement = {
      val labelName = ctx.freshNamesGenerator.nextName("while")
      val condition = whileStmt.getCondition.propagateAndCast(ctx)
      val body = whileStmt.getBody.propagateAndCast(ctx.copy(currentImplicitLabel = Some(labelName)))
      whileStmt.setCondition(condition)
      whileStmt.setBody(body)
      new LabeledStmt(labelName, whileStmt)
    }

    override def visit(n: ContinueStmt, ctx: Ctx): Statement = {
      val label = n.getLabel.propagateAndCast(ctx)
      n.setLabel(label)
    }

    override def visit(doStmt: DoStmt, ctx: Ctx): Statement = {
      val labelName = ctx.freshNamesGenerator.nextName("dowhile")
      val body = doStmt.getBody.propagateAndCast(ctx.copy(currentImplicitLabel = Some(labelName)))
      val condition = doStmt.getCondition.propagateAndCast(ctx)
      doStmt.setBody(body)
      doStmt.setCondition(condition)
      new LabeledStmt(labelName, doStmt)
    }

    override def visit(forEachStmt: ForEachStmt, ctx: Ctx): Statement = {
      val labelName = ctx.freshNamesGenerator.nextName("foreach")
      val variable = forEachStmt.getVariable.propagateAndCast(ctx)
      val iterable = forEachStmt.getIterable.propagateAndCast(ctx)
      val body = forEachStmt.getBody.propagateAndCast(ctx.copy(currentImplicitLabel = Some(labelName)))
      forEachStmt.setVariable(variable)
      forEachStmt.setIterable(iterable)
      forEachStmt.setBody(body)
      new LabeledStmt(labelName, forEachStmt)
    }

    override def visit(forStmt: ForStmt, ctx: Ctx): Statement = {
      val labelName = ctx.freshNamesGenerator.nextName("for")
      val init = forStmt.getInitialization.acceptThis(ctx)
      val compare = forStmt.getCompare.propagateAndCast(ctx)
      val update = forStmt.getUpdate.acceptThis(ctx)
      val body = forStmt.getBody.propagateAndCast(ctx.copy(currentImplicitLabel = Some(labelName)))
      forStmt.setInitialization(init)
      forStmt.setCompare(compare)
      forStmt.setUpdate(update)
      forStmt.setBody(body)
      new LabeledStmt(labelName, forStmt)
    }

    override def visit(n: ThrowStmt, ctx: Ctx): Statement = {
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setExpression(expr)
    }

    override def visit(n: SynchronizedStmt, ctx: Ctx): Statement = {
      ctx.er.reportErrorPos("'synchronized' found: JumboTrace does not support concurrency", NonFatalError, ctx.filename, n.getRange)
      new EmptyStmt(n.getTokenRange.getOrNull())
    }

    override def visit(n: TryStmt, ctx: Ctx): Statement = {
      val resources = n.getResources.acceptThis(ctx)
      val tryBlock = n.getTryBlock.propagateAndCast(ctx)
      val catchClauses = n.getCatchClauses.acceptThis(ctx)
      val finallyBlock = n.getFinallyBlock.propagateAndCast(ctx)
      n.setResources(resources)
      n.setTryBlock(tryBlock)
      n.setCatchClauses(catchClauses)
      n.setFinallyBlock(finallyBlock)
    }

    override def visit(n: CatchClause, ctx: Ctx): CatchClause = {
      val parameter = n.getParameter.propagateAndCast(ctx)
      val body = n.getBody.propagateAndCast(ctx)
      n.setParameter(parameter)
      n.setBody(body)
    }

    override def visit(n: LambdaExpr, ctx: Ctx): Expression = {
      val parameters = n.getParameters.acceptThis(ctx)
      val body = n.getBody.propagateAndCast(ctx)
      n.setParameters(parameters)
      n.setBody(body)
    }

    override def visit(n: MethodReferenceExpr, ctx: Ctx): Expression = {
      val scope = n.getScope.propagateAndCast(ctx)
      val typeArgs = n.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      n.setScope(scope)
      n.setTypeArguments(typeArgs)
    }

    override def visit(n: TypeExpr, ctx: Ctx): Expression = {
      val tpe = n.getType.propagateAndCast(ctx)
      n.setType(tpe)
    }

    override def visit(n: NodeList[_ <: Node], ctx: Ctx): Nothing = {
      throw new UnsupportedOperationException()
    }

    override def visit(n: Name, ctx: Ctx): Name = {
      val qualifier = n.getQualifier.propagateAndCast(ctx)
      n.setQualifier(qualifier)
    }

    override def visit(sn: SimpleName, ctx: Ctx): SimpleName = sn

    override def visit(n: ImportDeclaration, ctx: Ctx): ImportDeclaration = {
      val name = n.getName.propagateAndCast(ctx)
      n.setName(name)
    }

    override def visit(n: ModuleDeclaration, ctx: Ctx): ModuleDeclaration = {
      val annotations = n.getAnnotations.acceptThis(ctx)
      val name = n.getName.propagateAndCast(ctx)
      val directives = n.getDirectives.acceptThis(ctx)
      n.setAnnotations(annotations)
      n.setName(name)
      n.setDirectives(directives)
    }

    override def visit(n: ModuleRequiresDirective, ctx: Ctx): ModuleRequiresDirective = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val name = n.getName.propagateAndCast(ctx)
      n.setModifiers(modifiers)
      n.setName(name)
    }

    override def visit(n: ModuleExportsDirective, ctx: Ctx): ModuleExportsDirective = {
      val name = n.getName.propagateAndCast(ctx)
      val moduleNames = n.getModuleNames.acceptThis(ctx)
      n.setName(name)
      n.setModuleNames(moduleNames)
    }

    override def visit(n: ModuleProvidesDirective, ctx: Ctx): ModuleProvidesDirective = {
      val name = n.getName.propagateAndCast(ctx)
      val wth = n.getWith.acceptThis(ctx)
      n.setName(name)
      n.setWith(wth)
    }

    override def visit(n: ModuleUsesDirective, ctx: Ctx): ModuleUsesDirective = {
      val name = n.getName.propagateAndCast(ctx)
      n.setName(name)
    }

    override def visit(n: ModuleOpensDirective, ctx: Ctx): ModuleOpensDirective = {
      val name = n.getName.propagateAndCast(ctx)
      val moduleNames = n.getModuleNames.acceptThis(ctx)
      n.setName(name)
      n.setModuleNames(moduleNames)
    }

    override def visit(unparsableStmt: UnparsableStmt, ctx: Ctx): Statement = unparsableStmt

    override def visit(n: ReceiverParameter, ctx: Ctx): ReceiverParameter = {
      val annotations = n.getAnnotations.acceptThis(ctx)
      val tpe = n.getType.propagateAndCast(ctx)
      val name = n.getName.propagateAndCast(ctx)
      n.setAnnotations(annotations)
      n.setType(tpe)
      n.setName(name)
    }

    override def visit(varType: VarType, ctx: Ctx): VarType = varType

    override def visit(modifier: Modifier, ctx: Ctx): Modifier = modifier

    override def visit(n: SwitchExpr, ctx: Ctx): Expression = {
      val selector = n.getSelector.propagateAndCast(ctx)
      val entries = n.getEntries.acceptThis(ctx)
      n.setSelector(selector)
      n.setEntries(entries)
    }

    override def visit(n: YieldStmt, ctx: Ctx): Statement = {
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setExpression(expr)
    }

    override def visit(tbLitExpr: TextBlockLiteralExpr, ctx: Ctx): Expression = tbLitExpr

    override def visit(n: PatternExpr, ctx: Ctx): Expression = {
      val modifiers = n.getModifiers.acceptThis(ctx)
      val tpe = n.getType.propagateAndCast(ctx)
      val name = n.getName.propagateAndCast(ctx)
      n.setModifiers(modifiers)
      n.setType(tpe)
      n.setName(name)
    }

    extension[N <: Node] (ls: NodeList[N]) private def acceptThis(ctx: Ctx): NodeList[N] = {
      val copy = new ListBuffer[N]()
      ls.forEach(copy.addOne)
      ls.clear()
      for (node <- copy) {
        ls.add(node.propagateAndCast(ctx))
      }
      ls
    }

    private val objectType = StaticJavaParser.parseClassOrInterfaceType("Object")
    private val stringType = StaticJavaParser.parseClassOrInterfaceType("String")


    extension[T >: Null] (opt: Optional[T]) private def getOrNull(): T = {
      if opt.isPresent then opt.get() else null
    }

    extension[N <: Node] (n: N) private def propagateAndCast(ctx: Ctx): N = {
      n.accept(this, ctx).asInstanceOf[N]
    }

    extension[N >: Null <: Node] (opt: Optional[N]) private def propagateAndCast(ctx: Ctx): N = {
      opt.map(_.propagateAndCast(ctx)).getOrNull()
    }

    private def typeOf(expr: Expression, ctx: Ctx): Type = {
      ctx.typesMap.getOrElse(expr, {
        ctx.er.reportErrorPos(
          "could not find type of expression; falling back to Object (this may cause unnecessary wrapping and slightly slow down the program)",
          Warning,
          ctx.filename,
          expr.getRange
        )
        objectType
      })
    }

  }

}
