package s2sCompiler

import ErrorReporter.ErrorLevel
import ErrorReporter.ErrorLevel.*

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.*
import com.github.javaparser.ast.Node.PostOrderIterator
import com.github.javaparser.ast.`type`.*
import com.github.javaparser.ast.`type`.PrimitiveType.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.{BlockComment, JavadocComment, LineComment}
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.*
import com.github.javaparser.resolution.Resolvable
import com.github.javaparser.resolution.declarations.ResolvedDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.{JavaParserEnumConstantDeclaration, JavaParserFieldDeclaration, JavaParserParameterDeclaration, JavaParserPatternDeclaration, JavaParserVariableDeclaration}
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.{CombinedTypeSolver, JavaParserTypeSolver, ReflectionTypeSolver}
import injectionAutomation.InjectedMethods

import java.util.Optional
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Try

final class Transformer extends CompilerStage[Analyzer.Result, CompilationUnit] {

  override protected def runImpl(input: Analyzer.Result, errorReporter: ErrorReporter): Option[CompilationUnit] = {
    val Analyzer.Result(cu, typesMap, declMap, usedVariableNames) = input
    val output = cu.accept(
      new TransformationVisitor(),
      Ctx(
        errorReporter,
        cu.getStorage.map(_.getFileName).orElse("<unknown source>"),
        ListBuffer.empty,
        new FreshNamesGenerator(usedVariableNames),
        typesMap,
        declMap,
        currentlyExecutingMethodDescr = None,
        currentBreakTarget = None,
        currentContinueTarget = None,
        currentYieldTarget = None
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
                                declMap: Map[Resolvable[_], ResolvedDeclaration],
                                currentlyExecutingMethodDescr: Option[String],
                                currentBreakTarget: Option[Statement],
                                currentContinueTarget: Option[Statement],
                                currentYieldTarget: Option[SwitchExpr]
                              ) {
    def createAndSaveExtraVar(middleFix: String, tpe: Type): String = {
      val id = freshNamesGenerator.nextName(middleFix)
      extraVariables.addOne(new VariableDeclarator(tpe, id))
      id
    }
  }

  private final class TransformationVisitor extends GenericVisitor[Node, Ctx] {

    private type VariableDecl = JavaParserVariableDeclaration | JavaParserParameterDeclaration | JavaParserPatternDeclaration

    override def visit(cu: CompilationUnit, ctx: Ctx): CompilationUnit = {
      val packageDeclaration = cu.getPackageDeclaration.propagateAndCast(ctx)
      val importDeclarations = cu.getImports.acceptThis(ctx)
      val typeDeclarations = cu.getTypes.acceptThis(ctx)
      val moduleDeclaration = cu.getModule.propagateAndCast(ctx)
      cu.setPackageDeclaration(packageDeclaration)
      cu.setImports(importDeclarations)
      cu.setTypes(typeDeclarations)
      cu.setModule(moduleDeclaration)
    }

    override def visit(pDecl: PackageDeclaration, ctx: Ctx): PackageDeclaration = pDecl

    override def visit(tParam: TypeParameter, ctx: Ctx): TypeParameter = tParam

    override def visit(lc: LineComment, ctx: Ctx): LineComment = lc

    override def visit(bc: BlockComment, ctx: Ctx): BlockComment = bc

    override def visit(classOrInterfaceDecl: ClassOrInterfaceDeclaration, ctx: Ctx): ClassOrInterfaceDeclaration = {
      val modifiers = classOrInterfaceDecl.getModifiers.acceptThis(ctx)
      val annotations = classOrInterfaceDecl.getAnnotations.acceptThis(ctx)
      val name = visit(classOrInterfaceDecl.getName, ctx)
      val typeParameters = classOrInterfaceDecl.getTypeParameters.acceptThis(ctx)
      val extendedTypes = classOrInterfaceDecl.getExtendedTypes.acceptThis(ctx)
      val implementedTypes = classOrInterfaceDecl.getImplementedTypes.acceptThis(ctx)
      val permittedTypes = classOrInterfaceDecl.getPermittedTypes.acceptThis(ctx)
      val members = classOrInterfaceDecl.getMembers.acceptThis(ctx)
      classOrInterfaceDecl.setModifiers(modifiers)
      classOrInterfaceDecl.setAnnotations(annotations)
      classOrInterfaceDecl.setName(name)
      classOrInterfaceDecl.setTypeParameters(typeParameters)
      classOrInterfaceDecl.setExtendedTypes(extendedTypes)
      classOrInterfaceDecl.setImplementedTypes(implementedTypes)
      classOrInterfaceDecl.setPermittedTypes(permittedTypes)
      classOrInterfaceDecl.setMembers(members)
    }

    override def visit(recordDecl: RecordDeclaration, ctx: Ctx): RecordDeclaration = {
      val modifiers = recordDecl.getModifiers.acceptThis(ctx)
      val annotations = recordDecl.getAnnotations.acceptThis(ctx)
      val name = visit(recordDecl.getName, ctx)
      val parameters = recordDecl.getParameters.acceptThis(ctx)
      val typeParameters = recordDecl.getTypeParameters.acceptThis(ctx)
      val implementedTypes = recordDecl.getImplementedTypes.acceptThis(ctx)
      val members = recordDecl.getMembers.acceptThis(ctx)
      val receiverParameter = recordDecl.getReceiverParameter.propagateAndCast(ctx)
      recordDecl.setModifiers(modifiers)
      recordDecl.setAnnotations(annotations)
      recordDecl.setName(name)
      recordDecl.setParameters(parameters)
      recordDecl.setTypeParameters(typeParameters)
      recordDecl.setImplementedTypes(implementedTypes)
      recordDecl.setMembers(members)
      recordDecl.setReceiverParameter(receiverParameter)
    }

    override def visit(cConstrDecl: CompactConstructorDeclaration, externalCtx: Ctx): CompactConstructorDeclaration = {
      val id = cConstrDecl.getName.getIdentifier
      val variables = ListBuffer.empty[VariableDeclarator]
      val modifiers = cConstrDecl.getModifiers.acceptThis(externalCtx)
      val annotations = cConstrDecl.getAnnotations.acceptThis(externalCtx)
      val typeParameters = cConstrDecl.getTypeParameters.acceptThis(externalCtx)
      val name = visit(cConstrDecl.getName, externalCtx)
      val thrownExceptions = cConstrDecl.getThrownExceptions.acceptThis(externalCtx)
      val body = cConstrDecl.getBody.propagateAndCast(
        Ctx(
          externalCtx.er,
          externalCtx.filename,
          variables,
          externalCtx.freshNamesGenerator.emptyCopy,
          externalCtx.typesMap,
          externalCtx.declMap,
          currentlyExecutingMethodDescr = Some(s"compact constructor of $id"),
          currentBreakTarget = None,
          currentContinueTarget = None,
          currentYieldTarget = None
        )
      )
      declareAdditionalVarsInBody(variables, body)
      cConstrDecl.setModifiers(modifiers)
      cConstrDecl.setAnnotations(annotations)
      cConstrDecl.setTypeParameters(typeParameters)
      cConstrDecl.setName(name)
      cConstrDecl.setThrownExceptions(thrownExceptions)
      cConstrDecl.setBody(body)
    }

    override def visit(enumDecl: EnumDeclaration, ctx: Ctx): EnumDeclaration = {
      val modifiers = enumDecl.getModifiers.acceptThis(ctx)
      val annotations = enumDecl.getAnnotations.acceptThis(ctx)
      val name = visit(enumDecl.getName, ctx)
      val implementedTypes = enumDecl.getImplementedTypes.acceptThis(ctx)
      val entries = enumDecl.getEntries.acceptThis(ctx)
      val members = enumDecl.getMembers.acceptThis(ctx)
      enumDecl.setModifiers(modifiers)
      enumDecl.setAnnotations(annotations)
      enumDecl.setName(name)
      enumDecl.setImplementedTypes(implementedTypes)
      enumDecl.setEntries(entries)
      enumDecl.setMembers(members)
    }

    override def visit(enumConstDecl: EnumConstantDeclaration, ctx: Ctx): EnumConstantDeclaration = {
      val annotations = enumConstDecl.getAnnotations.acceptThis(ctx)
      val name = visit(enumConstDecl.getName, ctx)
      val arguments = enumConstDecl.getArguments.acceptThis(ctx)
      val body = enumConstDecl.getClassBody.acceptThis(ctx)
      enumConstDecl.setAnnotations(annotations)
      enumConstDecl.setName(name)
      enumConstDecl.setArguments(arguments)
      enumConstDecl.setClassBody(body)
    }

    override def visit(annotDecl: AnnotationDeclaration, ctx: Ctx): AnnotationDeclaration = {
      val modifiers = annotDecl.getModifiers.acceptThis(ctx)
      val annotations = annotDecl.getAnnotations.acceptThis(ctx)
      val name = visit(annotDecl.getName, ctx)
      val members = annotDecl.getMembers.acceptThis(ctx)
      annotDecl.setModifiers(modifiers)
      annotDecl.setAnnotations(annotations)
      annotDecl.setName(name)
      annotDecl.setMembers(members)
    }

    override def visit(annotMemberDecl: AnnotationMemberDeclaration, ctx: Ctx): AnnotationMemberDeclaration = {
      val modifiers = annotMemberDecl.getModifiers.acceptThis(ctx)
      val annotations = annotMemberDecl.getAnnotations.acceptThis(ctx)
      val tpe = annotMemberDecl.getType.propagateAndCast(ctx)
      val name = visit(annotMemberDecl.getName, ctx)
      val defaultValue = annotMemberDecl.getDefaultValue.propagateAndCast(ctx)
      annotMemberDecl.setModifiers(modifiers)
      annotMemberDecl.setAnnotations(annotations)
      annotMemberDecl.setType(tpe)
      annotMemberDecl.setName(name)
      annotMemberDecl.setDefaultValue(defaultValue)
    }

    override def visit(fieldDecl: FieldDeclaration, ctx: Ctx): FieldDeclaration = {
      val modifiers = fieldDecl.getModifiers.acceptThis(ctx)
      val annotations = fieldDecl.getAnnotations.acceptThis(ctx)
      val variables = fieldDecl.getVariables.acceptThis(ctx)
      fieldDecl.setModifiers(modifiers)
      fieldDecl.setAnnotations(annotations)
      fieldDecl.setVariables(variables)
    }

    override def visit(varDecl: VariableDeclarator, ctx: Ctx): VariableDeclarator = {
      val varId = varDecl.getName.getIdentifier
      val tpe = varDecl.getType.propagateAndCast(ctx)
      val name = visit(varDecl.getName, ctx)
      varDecl.setType(tpe)
      varDecl.setName(name)
      if (varDecl.getInitializer.isPresent) {
        val init = varDecl.getInitializer.get().propagateAndCast(ctx)
        varDecl.setInitializer(InjectedMethods.iVarWrite(init, varId))
      }
      varDecl
    }

    override def visit(constrDecl: ConstructorDeclaration, externalCtx: Ctx): ConstructorDeclaration = {
      val id = constrDecl.getName.getIdentifier
      val variables = ListBuffer.empty[VariableDeclarator]
      val modifiers = constrDecl.getModifiers.acceptThis(externalCtx)
      val annotations = constrDecl.getAnnotations.acceptThis(externalCtx)
      val typeParameters = constrDecl.getTypeParameters.acceptThis(externalCtx)
      val name = visit(constrDecl.getName, externalCtx)
      val parameters = constrDecl.getParameters.acceptThis(externalCtx)
      val thrownExceptions = constrDecl.getThrownExceptions.acceptThis(externalCtx)
      val body = constrDecl.getBody.propagateAndCast(
        Ctx(
          externalCtx.er,
          externalCtx.filename,
          variables,
          externalCtx.freshNamesGenerator.emptyCopy,
          externalCtx.typesMap,
          externalCtx.declMap,
          currentlyExecutingMethodDescr = Some(s"constructor of $id"),
          currentBreakTarget = None,
          currentContinueTarget = None,
          currentYieldTarget = None
        )
      )
      declareAdditionalVarsInBody(variables, body)
      val receiverParameter = constrDecl.getReceiverParameter.propagateAndCast(externalCtx)
      constrDecl.setModifiers(modifiers)
      constrDecl.setAnnotations(annotations)
      constrDecl.setTypeParameters(typeParameters)
      constrDecl.setName(name)
      constrDecl.setParameters(parameters)
      constrDecl.setThrownExceptions(thrownExceptions)
      constrDecl.setBody(body)
      constrDecl.setReceiverParameter(receiverParameter)
    }

    override def visit(methodDecl: MethodDeclaration, externalCtx: Ctx): MethodDeclaration = {
      val id = methodDecl.getName.getIdentifier
      val variables = ListBuffer.empty[VariableDeclarator]
      val modifiers = methodDecl.getModifiers.acceptThis(externalCtx)
      val annotations = methodDecl.getAnnotations.acceptThis(externalCtx)
      val typeParameters = methodDecl.getTypeParameters.acceptThis(externalCtx)
      val tpe = methodDecl.getType.propagateAndCast(externalCtx)
      val name = visit(methodDecl.getName, externalCtx)
      val parameters = methodDecl.getParameters.acceptThis(externalCtx)
      val thrownExceptions = methodDecl.getThrownExceptions.acceptThis(externalCtx)
      val body = methodDecl.getBody.propagateAndCast(
        Ctx(
          externalCtx.er,
          externalCtx.filename,
          variables,
          externalCtx.freshNamesGenerator.emptyCopy,
          externalCtx.typesMap,
          externalCtx.declMap,
          currentlyExecutingMethodDescr = Some(s"method $id"),
          currentBreakTarget = None,
          currentContinueTarget = None,
          currentYieldTarget = None
        )
      )
      declareAdditionalVarsInBody(variables, body)
      val receiverParameter = methodDecl.getReceiverParameter.propagateAndCast(externalCtx)
      methodDecl.setModifiers(modifiers)
      methodDecl.setAnnotations(annotations)
      methodDecl.setTypeParameters(typeParameters)
      methodDecl.setType(tpe)
      methodDecl.setName(name)
      methodDecl.setParameters(parameters)
      methodDecl.setThrownExceptions(thrownExceptions)
      methodDecl.setBody(body)
      methodDecl.setReceiverParameter(receiverParameter)
    }

    override def visit(param: Parameter, ctx: Ctx): Parameter = {
      val modifiers = param.getModifiers.acceptThis(ctx)
      val annotations = param.getAnnotations.acceptThis(ctx)
      val tpe = param.getType.propagateAndCast(ctx)
      val varArgsAnnotations = param.getVarArgsAnnotations.acceptThis(ctx)
      val name = visit(param.getName, ctx)
      param.setModifiers(modifiers)
      param.setAnnotations(annotations)
      param.setType(tpe)
      param.setVarArgsAnnotations(varArgsAnnotations)
      param.setName(name)
    }

    override def visit(initDecl: InitializerDeclaration, ctx: Ctx): InitializerDeclaration = {
      val body = initDecl.getBody.propagateAndCast(ctx)
      initDecl.setBody(body)
    }

    override def visit(jdocComment: JavadocComment, ctx: Ctx): JavadocComment = jdocComment

    override def visit(tpe: ClassOrInterfaceType, ctx: Ctx): ClassOrInterfaceType = tpe

    override def visit(tpe: PrimitiveType, ctx: Ctx): PrimitiveType = tpe

    override def visit(tpe: ArrayType, ctx: Ctx): ArrayType = tpe

    override def visit(arrayCreationLevel: ArrayCreationLevel, ctx: Ctx): ArrayCreationLevel = {
      val dimension = arrayCreationLevel.getDimension.propagateAndCast(ctx)
      val annotations = arrayCreationLevel.getAnnotations.acceptThis(ctx)
      arrayCreationLevel.setDimension(dimension)
      arrayCreationLevel.setAnnotations(annotations)
    }

    override def visit(tpe: IntersectionType, ctx: Ctx): IntersectionType = tpe

    override def visit(tpe: UnionType, ctx: Ctx): UnionType = tpe

    override def visit(tpe: VoidType, ctx: Ctx): VoidType = tpe

    override def visit(tpe: WildcardType, ctx: Ctx): WildcardType = tpe

    override def visit(tpe: UnknownType, ctx: Ctx): UnknownType = tpe

    override def visit(arrayAccessExpr: ArrayAccessExpr, ctx: Ctx): Expression = {
      import ctx.{er, filename}
      val arrayType = typeOf(arrayAccessExpr.getName, ctx)
      val arrayVarId = ctx.createAndSaveExtraVar("array", arrayType)
      val idxVarId = ctx.createAndSaveExtraVar("index", intType())
      val name = arrayAccessExpr.getName.propagateAndCast(ctx)
      val index = arrayAccessExpr.getIndex.propagateAndCast(ctx)
      arrayAccessExpr.setName(new EnclosedExpr(new AssignExpr(new NameExpr(arrayVarId), name, AssignExpr.Operator.ASSIGN)))
      arrayAccessExpr.setIndex(new AssignExpr(new NameExpr(idxVarId), index, AssignExpr.Operator.ASSIGN))
      InjectedMethods.iArrayRead(arrayAccessExpr, arrayVarId, idxVarId)
    }

    override def visit(arrayCreationExpr: ArrayCreationExpr, ctx: Ctx): Expression = {
      val elemType = arrayCreationExpr.getElementType.propagateAndCast(ctx)
      val levels = arrayCreationExpr.getLevels.acceptThis(ctx)
      val initializerExpr = arrayCreationExpr.getInitializer.propagateAndCast(ctx)
      arrayCreationExpr.setElementType(elemType)
      arrayCreationExpr.setLevels(levels)
      arrayCreationExpr.setInitializer(initializerExpr)
    }

    override def visit(arrayInitExpr: ArrayInitializerExpr, ctx: Ctx): Expression = {
      val values = arrayInitExpr.getValues.acceptThis(ctx)
      arrayInitExpr.setValues(values)
    }

    override def visit(assignExpr: AssignExpr, ctx: Ctx): Expression = {
      assignExpr.getTarget match {
        case nameExpr: NameExpr => {
          ctx.declMap.get(nameExpr) match {
            case Some(resolvedVariableDeclaration: VariableDecl) => {
              val value = assignExpr.getValue.propagateAndCast(ctx)
              assignExpr.setValue(value)
              InjectedMethods.iVarWrite(assignExpr, nameExpr.getName.getIdentifier)
            }
            case _ =>
              InjectedMethods.iInstanceFieldWrite(assignExpr, "this", nameExpr.getName.getIdentifier)
          }
        }
        case fieldAccessExpr: FieldAccessExpr => {
          val scope = fieldAccessExpr.getScope.propagateAndCast(ctx)
          ctx.declMap.get(fieldAccessExpr) match {
            case Some(fieldDeclaration: JavaParserFieldDeclaration) if fieldDeclaration.isStatic => {
              fieldAccessExpr.setScope(scope)
              InjectedMethods.iStaticFieldWrite(assignExpr, fieldDeclaration.declaringType().getClassName,
                fieldAccessExpr.getName.getIdentifier)
            }
            case Some(fieldDeclaration: JavaParserFieldDeclaration) => {
              val receiverType = typeOf(fieldAccessExpr.getScope, ctx)
              val receiverVarId = ctx.createAndSaveExtraVar("receiver", receiverType)
              fieldAccessExpr.setScope(new EnclosedExpr(new AssignExpr(new NameExpr(receiverVarId), scope, AssignExpr.Operator.ASSIGN)))
              InjectedMethods.iInstanceFieldWrite(assignExpr, receiverVarId, fieldAccessExpr.getName.getIdentifier)
            }
            case unexpected => throw new AssertionError(s"unexpected: $unexpected")
          }
        }
        case arrayAccessExpr: ArrayAccessExpr => {
          val arrayType = typeOf(arrayAccessExpr.getName, ctx)
          val arrayVarId = ctx.createAndSaveExtraVar("array", arrayType)
          val idxVarId = ctx.createAndSaveExtraVar("index", intType())
          val name = arrayAccessExpr.getName.propagateAndCast(ctx)
          val index = arrayAccessExpr.getIndex.propagateAndCast(ctx)
          val value = assignExpr.getValue.propagateAndCast(ctx)
          arrayAccessExpr.setName(new EnclosedExpr(new AssignExpr(new NameExpr(arrayVarId), name, AssignExpr.Operator.ASSIGN)))
          arrayAccessExpr.setIndex(new AssignExpr(new NameExpr(idxVarId), index, AssignExpr.Operator.ASSIGN))
          assignExpr.setValue(value)
          InjectedMethods.iArrayWrite(assignExpr, arrayVarId, idxVarId)
        }
        case unexpected => throw new AssertionError(
          s"unexpected lhs of assignment: ${unexpected.getClass.getSimpleName} ($unexpected)"
        )
      }
    }

    override def visit(binaryExpr: BinaryExpr, ctx: Ctx): Expression = {
      val left = binaryExpr.getLeft.propagateAndCast(ctx)
      val right = binaryExpr.getRight.propagateAndCast(ctx)
      binaryExpr.setLeft(left)
      binaryExpr.setRight(right)
    }

    override def visit(castExpr: CastExpr, ctx: Ctx): Expression = {
      val tpe = castExpr.getType.propagateAndCast(ctx)
      val expr = castExpr.getExpression.propagateAndCast(ctx)
      castExpr.setType(tpe)
      castExpr.setExpression(expr)
    }

    override def visit(classExpr: ClassExpr, ctx: Ctx): Expression = {
      val tpe = classExpr.getType.propagateAndCast(ctx)
      classExpr.setType(tpe)
    }

    override def visit(conditionalExpr: ConditionalExpr, ctx: Ctx): Expression = {
      val condition = conditionalExpr.getCondition.propagateAndCast(ctx)
      val thenExpr = conditionalExpr.getThenExpr.propagateAndCast(ctx)
      val elseExpr = conditionalExpr.getElseExpr.propagateAndCast(ctx)
      conditionalExpr.setCondition(condition)
      conditionalExpr.setThenExpr(thenExpr)
      conditionalExpr.setElseExpr(elseExpr)
    }

    override def visit(enclosedExpr: EnclosedExpr, ctx: Ctx): Expression = {
      val inner = enclosedExpr.getInner.propagateAndCast(ctx)
      enclosedExpr.setInner(inner)
    }

    override def visit(fieldAccessExpr: FieldAccessExpr, ctx: Ctx): Expression = {
      val scopeTypeOpt = ctx.typesMap.get(fieldAccessExpr.getScope)
      val fieldId = fieldAccessExpr.getName.getIdentifier
      val scope = fieldAccessExpr.getScope.propagateAndCast(ctx)
      val typeArgs = fieldAccessExpr.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      val name = visit(fieldAccessExpr.getName, ctx)
      fieldAccessExpr.setTypeArguments(typeArgs)
      fieldAccessExpr.setName(name)
      ctx.declMap.get(fieldAccessExpr) match {
        case Some(fieldDeclaration: JavaParserFieldDeclaration) if fieldDeclaration.isStatic => {
          fieldAccessExpr.setScope(scope)
          InjectedMethods.iStaticFieldRead(fieldAccessExpr, fieldDeclaration.declaringType().getClassName, fieldId)
        }
        case Some(decl) if (decl.isInstanceOf[JavaParserFieldDeclaration]
          || (scopeTypeOpt.exists(_.isArrayType) && fieldId == "length")) => {
          val receiverId = ctx.createAndSaveExtraVar("receiver", typeOf(fieldAccessExpr.getScope, ctx))
          fieldAccessExpr.setScope(new EnclosedExpr(new AssignExpr(new NameExpr(receiverId), scope, AssignExpr.Operator.ASSIGN)))
          InjectedMethods.iInstanceFieldRead(fieldAccessExpr, receiverId, fieldId)
        }
        case Some(enumConstantDeclaration: JavaParserEnumConstantDeclaration) => {
          fieldAccessExpr.setScope(scope)
          InjectedMethods.iStaticFieldRead(fieldAccessExpr, enumConstantDeclaration.getType.describe(), fieldId)
        }
        case Some(reflectionFieldDeclaration: ReflectionFieldDeclaration) => {
          fieldAccessExpr.setScope(scope)
          InjectedMethods.iStaticFieldRead(fieldAccessExpr, reflectionFieldDeclaration.declaringType().getClassName, fieldId)
        }
        case unexpected =>
          throw new AssertionError(s"unexpected: $unexpected")
      }
    }

    override def visit(instanceOfExpr: InstanceOfExpr, ctx: Ctx): Expression = {
      val expr = instanceOfExpr.getExpression.propagateAndCast(ctx)
      val tpe = instanceOfExpr.getType.propagateAndCast(ctx)
      val pattern = instanceOfExpr.getPattern.propagateAndCast(ctx)
      instanceOfExpr.setExpression(expr)
      instanceOfExpr.setType(tpe)
      instanceOfExpr.setPattern(pattern)
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

    override def visit(nameExpr: NameExpr, ctx: Ctx): Expression = {
      import ctx.{freshNamesGenerator, extraVariables, declMap}
      val name = visit(nameExpr.getName, ctx)
      nameExpr.setName(name)
      declMap.get(nameExpr) match
        case Some(_: VariableDecl) =>
          InjectedMethods.iVarRead(nameExpr, name.getIdentifier)
        case Some(_: JavaParserFieldDeclaration) =>
          InjectedMethods.iInstanceFieldRead(nameExpr, "this", nameExpr.getName.getIdentifier)
        case _ => nameExpr // TODO other cases?
    }

    override def visit(objectCreationExpr: ObjectCreationExpr, ctx: Ctx): Expression = {
      val scope = objectCreationExpr.getScope.propagateAndCast(ctx)
      val tpe = objectCreationExpr.getType.propagateAndCast(ctx)
      val typeArgs = objectCreationExpr.getTypeArguments.map(_.acceptThis(ctx)).getOrNull()
      val args = objectCreationExpr.getArguments.acceptThis(ctx)
      val anonymousClassBody = objectCreationExpr.getAnonymousClassBody.map(_.acceptThis(ctx)).getOrNull()
      objectCreationExpr.setScope(scope)
      objectCreationExpr.setType(tpe)
      objectCreationExpr.setTypeArguments(typeArgs)
      objectCreationExpr.setArguments(args)
      objectCreationExpr.setAnonymousClassBody(anonymousClassBody)
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

    override def visit(assertStmt: AssertStmt, ctx: Ctx): Statement = {
      val check = assertStmt.getCheck.propagateAndCast(ctx)
      val msg = assertStmt.getMessage.propagateAndCast(ctx)
      assertStmt.setCheck(check)
      assertStmt.setMessage(msg)
    }

    override def visit(n: BlockStmt, ctx: Ctx): Statement = {
      val stats = n.getStatements.acceptThis(ctx)
      n.setStatements(stats)
    }

    override def visit(labeledStmt: LabeledStmt, ctx: Ctx): Statement = {
      val label = labeledStmt.getLabel.propagateAndCast(ctx)
      val stat = labeledStmt.getStatement.propagateAndCast(ctx)
      labeledStmt.setLabel(label)
      labeledStmt.setStatement(stat)
    }

    override def visit(emptyStmt: EmptyStmt, ctx: Ctx): Statement = emptyStmt

    override def visit(expressionStmt: ExpressionStmt, ctx: Ctx): Statement = {
      val expr = expressionStmt.getExpression.propagateAndCast(ctx)
      expressionStmt.setExpression(expr)
    }

    override def visit(switchStmt: SwitchStmt, ctx: Ctx): Statement = {
      val selector = switchStmt.getSelector.propagateAndCast(ctx)
      val entries = switchStmt.getEntries.acceptThis(ctx.copy(currentBreakTarget = Some(switchStmt)))
      switchStmt.setSelector(selector)
      switchStmt.setEntries(entries)
    }

    override def visit(n: SwitchEntry, ctx: Ctx): SwitchEntry = {
      val labels = n.getLabels.acceptThis(ctx)
      val statements = n.getStatements.acceptThis(ctx)
      n.setLabels(labels)
      n.setStatements(statements)
    }

    override def visit(breakStmt: BreakStmt, ctx: Ctx): Statement = {
      val label = breakStmt.getLabel.propagateAndCast(ctx)
      breakStmt.setLabel(label)
    }

    override def visit(n: ReturnStmt, ctx: Ctx): Statement = {
      val expr = n.getExpression.propagateAndCast(ctx)
      n.setExpression(expr)
    }

    override def visit(ifStmt: IfStmt, ctx: Ctx): Statement = {
      val condition = ifStmt.getCondition.propagateAndCast(ctx)
      val thenStat = ifStmt.getThenStmt.propagateAndCast(ctx).makeBlockIfNotAlready
      ifStmt.setCondition(condition)
      ifStmt.setThenStmt(thenStat)
      if (ifStmt.getElseStmt.isPresent) {
        val elseStat = ifStmt.getElseStmt.propagateAndCast(ctx).makeBlockIfNotAlready
        ifStmt.setElseStmt(elseStat)
      }
      ifStmt
    }

    override def visit(whileStmt: WhileStmt, ctx: Ctx): Statement = {
      val condition = whileStmt.getCondition.propagateAndCast(ctx)
      val body = whileStmt.getBody.propagateAndCast(
        ctx.copy(currentBreakTarget = Some(whileStmt), currentContinueTarget = Some(whileStmt))
      ).makeBlockIfNotAlready
      whileStmt.setCondition(condition)
      whileStmt.setBody(body)
    }

    override def visit(continueStmt: ContinueStmt, ctx: Ctx): Statement = {
      val label = continueStmt.getLabel.propagateAndCast(ctx)
      continueStmt.setLabel(label)
    }

    override def visit(doStmt: DoStmt, ctx: Ctx): Statement = {
      val body = doStmt.getBody.propagateAndCast(ctx.copy(
        currentBreakTarget = Some(doStmt), currentContinueTarget = Some(doStmt))
      ).makeBlockIfNotAlready
      val condition = doStmt.getCondition.propagateAndCast(ctx)
      doStmt.setBody(body)
      doStmt.setCondition(condition)
    }

    override def visit(forEachStmt: ForEachStmt, ctx: Ctx): Statement = {
      val variable = forEachStmt.getVariable.propagateAndCast(ctx)
      val iterable = forEachStmt.getIterable.propagateAndCast(ctx)
      val body = forEachStmt.getBody.propagateAndCast(ctx.copy(
        currentBreakTarget = Some(forEachStmt), currentContinueTarget = Some(forEachStmt)
      )).makeBlockIfNotAlready
      forEachStmt.setVariable(variable)
      forEachStmt.setIterable(iterable)
      forEachStmt.setBody(body)
    }

    override def visit(forStmt: ForStmt, ctx: Ctx): Statement = {
      val init = forStmt.getInitialization.acceptThis(ctx)
      val compare = forStmt.getCompare.propagateAndCast(ctx)
      val update = forStmt.getUpdate.acceptThis(ctx)
      val body = forStmt.getBody.propagateAndCast(ctx.copy(
        currentBreakTarget = Some(forStmt), currentContinueTarget = Some(forStmt)
      )).makeBlockIfNotAlready
      forStmt.setInitialization(init)
      forStmt.setCompare(compare)
      forStmt.setUpdate(update)
      forStmt.setBody(body)
    }

    override def visit(throwStmt: ThrowStmt, ctx: Ctx): Statement = {
      val expr = throwStmt.getExpression.propagateAndCast(ctx)
      throwStmt.setExpression(expr)
    }

    override def visit(synchronizedStmt: SynchronizedStmt, ctx: Ctx): Statement = {
      ctx.er.reportErrorPos("'synchronized' found: JumboTrace does not support concurrency", NonFatalError, ctx.filename, synchronizedStmt.getRange)
      val expr = synchronizedStmt.getExpression.propagateAndCast(ctx)
      val body = synchronizedStmt.getBody.propagateAndCast(ctx)
      synchronizedStmt.setExpression(expr)
      synchronizedStmt.setBody(body)
    }

    override def visit(tryStmt: TryStmt, ctx: Ctx): Statement = {
      val resources = tryStmt.getResources.acceptThis(ctx)
      val tryBlock = tryStmt.getTryBlock.propagateAndCast(ctx)
      val catchClauses = tryStmt.getCatchClauses.acceptThis(ctx)
      val finallyBlock = tryStmt.getFinallyBlock.propagateAndCast(ctx)
      tryStmt.setResources(resources)
      tryStmt.setTryBlock(tryBlock)
      tryStmt.setCatchClauses(catchClauses)
      tryStmt.setFinallyBlock(finallyBlock)
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

    override def visit(name: Name, ctx: Ctx): Name = {
      val qualifier = name.getQualifier.propagateAndCast(ctx)
      name.setQualifier(qualifier)
    }

    override def visit(sn: SimpleName, ctx: Ctx): SimpleName = sn

    override def visit(importDecl: ImportDeclaration, ctx: Ctx): ImportDeclaration = {
      val name = importDecl.getName.propagateAndCast(ctx)
      importDecl.setName(name)
    }

    override def visit(moduleDecl: ModuleDeclaration, ctx: Ctx): ModuleDeclaration = {
      val annotations = moduleDecl.getAnnotations.acceptThis(ctx)
      val name = moduleDecl.getName.propagateAndCast(ctx)
      val directives = moduleDecl.getDirectives.acceptThis(ctx)
      moduleDecl.setAnnotations(annotations)
      moduleDecl.setName(name)
      moduleDecl.setDirectives(directives)
    }

    override def visit(moduleReqDecl: ModuleRequiresDirective, ctx: Ctx): ModuleRequiresDirective = {
      val modifiers = moduleReqDecl.getModifiers.acceptThis(ctx)
      val name = moduleReqDecl.getName.propagateAndCast(ctx)
      moduleReqDecl.setModifiers(modifiers)
      moduleReqDecl.setName(name)
    }

    override def visit(moduleExportsDirective: ModuleExportsDirective, ctx: Ctx): ModuleExportsDirective = {
      val name = moduleExportsDirective.getName.propagateAndCast(ctx)
      val moduleNames = moduleExportsDirective.getModuleNames.acceptThis(ctx)
      moduleExportsDirective.setName(name)
      moduleExportsDirective.setModuleNames(moduleNames)
    }

    override def visit(moduleProviedsDirective: ModuleProvidesDirective, ctx: Ctx): ModuleProvidesDirective = {
      val name = moduleProviedsDirective.getName.propagateAndCast(ctx)
      val wth = moduleProviedsDirective.getWith.acceptThis(ctx)
      moduleProviedsDirective.setName(name)
      moduleProviedsDirective.setWith(wth)
    }

    override def visit(moduleUsesDirective: ModuleUsesDirective, ctx: Ctx): ModuleUsesDirective = {
      val name = moduleUsesDirective.getName.propagateAndCast(ctx)
      moduleUsesDirective.setName(name)
    }

    override def visit(moduleOpensDirective: ModuleOpensDirective, ctx: Ctx): ModuleOpensDirective = {
      val name = moduleOpensDirective.getName.propagateAndCast(ctx)
      val moduleNames = moduleOpensDirective.getModuleNames.acceptThis(ctx)
      moduleOpensDirective.setName(name)
      moduleOpensDirective.setModuleNames(moduleNames)
    }

    override def visit(unparsableStmt: UnparsableStmt, ctx: Ctx): Statement = unparsableStmt

    override def visit(receiverParameter: ReceiverParameter, ctx: Ctx): ReceiverParameter = {
      val annotations = receiverParameter.getAnnotations.acceptThis(ctx)
      val tpe = receiverParameter.getType.propagateAndCast(ctx)
      val name = receiverParameter.getName.propagateAndCast(ctx)
      receiverParameter.setAnnotations(annotations)
      receiverParameter.setType(tpe)
      receiverParameter.setName(name)
    }

    override def visit(varType: VarType, ctx: Ctx): VarType = varType

    override def visit(modifier: Modifier, ctx: Ctx): Modifier = modifier

    override def visit(switchExpr: SwitchExpr, ctx: Ctx): Expression = {
      val selector = switchExpr.getSelector.propagateAndCast(ctx)
      val entries = switchExpr.getEntries.acceptThis(ctx.copy(currentYieldTarget = Some(switchExpr)))
      switchExpr.setSelector(selector)
      switchExpr.setEntries(entries)
    }

    override def visit(yieldStmt: YieldStmt, ctx: Ctx): Statement = {
      val expr = yieldStmt.getExpression.propagateAndCast(ctx)
      yieldStmt.setExpression(expr)
    }

    override def visit(tbLitExpr: TextBlockLiteralExpr, ctx: Ctx): Expression = tbLitExpr

    override def visit(patternExpr: PatternExpr, ctx: Ctx): Expression = {
      val modifiers = patternExpr.getModifiers.acceptThis(ctx)
      val tpe = patternExpr.getType.propagateAndCast(ctx)
      val name = patternExpr.getName.propagateAndCast(ctx)
      patternExpr.setModifiers(modifiers)
      patternExpr.setType(tpe)
      patternExpr.setName(name)
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

    private val objectType = StaticJavaParser.parseClassOrInterfaceType("java.lang.Object")
    private val stringType = StaticJavaParser.parseClassOrInterfaceType("java.lang.String")


    extension[T >: Null] (opt: Optional[T]) private def getOrNull(): T = {
      if opt.isPresent then opt.get() else null
    }

    extension[N <: Node] (n: N) private def propagateAndCast(ctx: Ctx): N = {
      n.accept(this, ctx).asInstanceOf[N]
    }

    extension[N >: Null <: Node] (opt: Optional[N]) private def propagateAndCast(ctx: Ctx): N = {
      opt.map(_.propagateAndCast(ctx)).getOrNull()
    }

    extension (stat: Statement) private def makeBlockIfNotAlready: BlockStmt = {
      stat match {
        case blockStmt: BlockStmt => blockStmt
        case _ => new BlockStmt().addStatement(stat)
      }
    }

    private def typeOf(expr: Expression, ctx: Ctx): Type = {
      ctx.typesMap.getOrElse(expr, {
        ctx.er.reportErrorPos(
          "could not find type of expression; falling back to Object (this may cause unnecessary wrapping and " +
            "slightly slow down the program)",
          Warning,
          ctx.filename,
          expr.getRange
        )
        objectType
      })
    }

  }

  private def declareAdditionalVarsInBody(variables: ListBuffer[VariableDeclarator], body: BlockStmt): Unit = {
    for (varDecl <- variables.reverseIterator) { // reversing just for convenience, it would work without too
      body.getStatements.addFirst(new ExpressionStmt(new VariableDeclarationExpr(varDecl)))
    }
  }

}
