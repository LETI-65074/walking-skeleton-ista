## 🎥 Vídeo do Projeto


🔗 [Assista ao vídeo no YouTube](https://youtu.be/BWfPpgWG4os?si=sQgE1kjTwVhoelhu)

Este repositório usa um workflow do GitHub Actions que, a cada push na branch main (ou por execução manual via workflow_dispatch), faz o build de produção da aplicação (perfil -Pproduction, Java 21, Maven) e publica o JAR como artefacto da execução.
O que o workflow faz:

•	Checkout do código com histórico completo (compatível com passos que precisem de histórico).

•	Configuração do JDK 21 (Temurin) com cache de dependências Maven.

•	Build Maven de produção: mvn -B -Pproduction -Dmaven.test.skip=true clean package
(gera o JAR em target/).

•	Publicação do artefacto: o(s) target/*.jar é(são) carregado(s) nos artefactos da execução, com retenção de 7 dias.
Gatilhos e salvaguardas:

•	Executa em push para main; pode ser corrido manualmente (Run workflow).

•	paths-ignore: "*.jar" evita loops caso JARs sejam adicionados ao repositório.
Como obter o JAR:

•	Ir a Actions → (execução mais recente) → Artifacts e descarregar o ficheiro publicado.
Benefício: garante build reprodutível e empacotamento automático em cada alteração integrada na main, centralizando a distribuição do binário diretamente nas execuções do Actions.

Excerto do workflow (`.github/workflows/build.yml`)

name: Build JAR (Vaadin Prod + Commit JAR)


on:
  push:
    branches: [ main ]
    paths-ignore:
      - "*.jar"                 # evita loop quando o bot commita o JAR
  workflow_dispatch: {}


# Permissão para o GITHUB_TOKEN poder fazer push de commits
permissions:
  contents: write


jobs:
  build:
    runs-on: ubuntu-latest


    steps:
      - name: Checkout do código
        uses: actions/checkout@v4
        with:
          fetch-depth: 0        # necessário para fazer commit & push


      - name: Configurar JDK 21
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven


      - name: Build Maven (produção)
        run: |
          if [ -f ./mvnw ]; then
            ./mvnw -B -Pproduction -Dmaven.test.skip=true clean package
          else
            mvn -B -Pproduction -Dmaven.test.skip=true clean package
          fi

     


      - name: Publicar artefacto (.jar)
        uses: actions/upload-artifact@v4
        with:
          name: app-jar-${{ github.ref_name }}-${{ github.run_number }}
          path: |
            **/target/*.jar
            ./*.jar
          if-no-files-found: error
          retention-days: 7

Teste para jar files 
