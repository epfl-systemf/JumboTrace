import com.github.javaparser.ast.`type`.*
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.comments.{BlockComment, JavadocComment, LineComment}
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.modules.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.ast.*

final class TreeTransformer extends CompilerStage[CompilationUnit, CompilationUnit] {

  override protected def runImpl(cu: CompilationUnit, errorReporter: ErrorReporter): Option[CompilationUnit] = {
    // TODO
    Some(cu)
  }

  private final class TransformationVisitor extends VoidVisitorAdapter[VisitorCtx] {

    override def visit(n: NodeList[_ <: Node], er: VisitorCtx): Unit = {
      super.visit(n, er)
    }

    override def visit(n: AnnotationDeclaration, er: VisitorCtx): Unit = {
      super.visit(n, er)
    }

    override def visit(n: AnnotationMemberDeclaration, er: VisitorCtx): Unit = {
      super.visit(n, er)
    }

    override def visit(n: ArrayAccessExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ArrayCreationExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ArrayCreationLevel, er: VisitorCtx): Unit = ???

    override def visit(n: ArrayInitializerExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ArrayType, er: VisitorCtx): Unit = ???

    override def visit(n: AssertStmt, er: VisitorCtx): Unit = ???

    override def visit(n: AssignExpr, er: VisitorCtx): Unit = ???

    override def visit(n: BinaryExpr, er: VisitorCtx): Unit = ???

    override def visit(n: BlockComment, er: VisitorCtx): Unit = ???

    override def visit(n: BlockStmt, er: VisitorCtx): Unit = ???

    override def visit(n: BooleanLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: BreakStmt, er: VisitorCtx): Unit = ???

    override def visit(n: CastExpr, er: VisitorCtx): Unit = ???

    override def visit(n: CatchClause, er: VisitorCtx): Unit = ???

    override def visit(n: CharLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ClassExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ClassOrInterfaceDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: ClassOrInterfaceType, er: VisitorCtx): Unit = ???

    override def visit(n: CompilationUnit, er: VisitorCtx): Unit = ???

    override def visit(n: ConditionalExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ConstructorDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: ContinueStmt, er: VisitorCtx): Unit = ???

    override def visit(n: DoStmt, er: VisitorCtx): Unit = ???

    override def visit(n: DoubleLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: EmptyStmt, er: VisitorCtx): Unit = ???

    override def visit(n: EnclosedExpr, er: VisitorCtx): Unit = ???

    override def visit(n: EnumConstantDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: EnumDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: ExplicitConstructorInvocationStmt, er: VisitorCtx): Unit = ???

    override def visit(n: ExpressionStmt, er: VisitorCtx): Unit = ???

    override def visit(n: FieldAccessExpr, er: VisitorCtx): Unit = ???

    override def visit(n: FieldDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: ForStmt, er: VisitorCtx): Unit = ???

    override def visit(n: ForEachStmt, er: VisitorCtx): Unit = ???

    override def visit(n: IfStmt, er: VisitorCtx): Unit = ???

    override def visit(n: ImportDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: InitializerDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: InstanceOfExpr, er: VisitorCtx): Unit = ???

    override def visit(n: IntegerLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: IntersectionType, er: VisitorCtx): Unit = ???

    override def visit(n: JavadocComment, er: VisitorCtx): Unit = ???

    override def visit(n: LabeledStmt, er: VisitorCtx): Unit = ???

    override def visit(n: LambdaExpr, er: VisitorCtx): Unit = ???

    override def visit(n: LineComment, er: VisitorCtx): Unit = ???

    override def visit(n: LocalClassDeclarationStmt, er: VisitorCtx): Unit = ???

    override def visit(n: LocalRecordDeclarationStmt, er: VisitorCtx): Unit = ???

    override def visit(n: LongLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: MarkerAnnotationExpr, er: VisitorCtx): Unit = ???

    override def visit(n: MemberValuePair, er: VisitorCtx): Unit = ???

    override def visit(n: MethodCallExpr, er: VisitorCtx): Unit = ???

    override def visit(n: MethodDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: MethodReferenceExpr, er: VisitorCtx): Unit = ???

    override def visit(n: NameExpr, er: VisitorCtx): Unit = ???

    override def visit(n: Name, er: VisitorCtx): Unit = ???

    override def visit(n: NormalAnnotationExpr, er: VisitorCtx): Unit = ???

    override def visit(n: NullLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ObjectCreationExpr, er: VisitorCtx): Unit = ???

    override def visit(n: PackageDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: Parameter, er: VisitorCtx): Unit = ???

    override def visit(n: PrimitiveType, er: VisitorCtx): Unit = ???

    override def visit(n: RecordDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: CompactConstructorDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: ReturnStmt, er: VisitorCtx): Unit = ???

    override def visit(n: SimpleName, er: VisitorCtx): Unit = ???

    override def visit(n: SingleMemberAnnotationExpr, er: VisitorCtx): Unit = ???

    override def visit(n: StringLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(n: SuperExpr, er: VisitorCtx): Unit = ???

    override def visit(n: SwitchEntry, er: VisitorCtx): Unit = ???

    override def visit(n: SwitchStmt, er: VisitorCtx): Unit = ???

    override def visit(n: SynchronizedStmt, er: VisitorCtx): Unit = ???

    override def visit(n: ThisExpr, er: VisitorCtx): Unit = ???

    override def visit(n: ThrowStmt, er: VisitorCtx): Unit = ???

    override def visit(n: TryStmt, er: VisitorCtx): Unit = ???

    override def visit(n: TypeExpr, er: VisitorCtx): Unit = ???

    override def visit(n: TypeParameter, er: VisitorCtx): Unit = ???

    override def visit(n: UnaryExpr, er: VisitorCtx): Unit = ???

    override def visit(n: UnionType, er: VisitorCtx): Unit = ???

    override def visit(n: UnknownType, er: VisitorCtx): Unit = ???

    override def visit(n: VariableDeclarationExpr, er: VisitorCtx): Unit = ???

    override def visit(n: VariableDeclarator, er: VisitorCtx): Unit = ???

    override def visit(n: VoidType, er: VisitorCtx): Unit = ???

    override def visit(n: WhileStmt, er: VisitorCtx): Unit = ???

    override def visit(n: WildcardType, er: VisitorCtx): Unit = ???

    override def visit(n: ModuleDeclaration, er: VisitorCtx): Unit = ???

    override def visit(n: ModuleRequiresDirective, er: VisitorCtx): Unit = ???

    override def visit(n: ModuleExportsDirective, er: VisitorCtx): Unit = ???

    override def visit(n: ModuleProvidesDirective, er: VisitorCtx): Unit = ???

    override def visit(n: ModuleUsesDirective, er: VisitorCtx): Unit = ???

    override def visit(n: ModuleOpensDirective, er: VisitorCtx): Unit = ???

    override def visit(n: UnparsableStmt, er: VisitorCtx): Unit = ???

    override def visit(n: ReceiverParameter, er: VisitorCtx): Unit = ???

    override def visit(n: VarType, er: VisitorCtx): Unit = ???

    override def visit(n: Modifier, er: VisitorCtx): Unit = ???

    override def visit(switchExpr: SwitchExpr, er: VisitorCtx): Unit = ???

    override def visit(n: TextBlockLiteralExpr, er: VisitorCtx): Unit = ???

    override def visit(yieldStmt: YieldStmt, er: VisitorCtx): Unit = ???

    override def visit(n: PatternExpr, er: VisitorCtx): Unit = ???
  }

  private final case class VisitorCtx(er: ErrorReporter)

}
