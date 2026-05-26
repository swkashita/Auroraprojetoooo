package com.ifsp.Auroraprojeto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuroraController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ConteudoRepository conteudoRepository;

    @Autowired
    private CalendarioRepository calendarioRepository;
    
    @Autowired
    private AvisoRepository avisoRepository; 

    @Autowired
    private ProgressoAulaRepository progressoAulaRepository;

    // CREDENCIAIS FIXAS DO ADMINISTRADOR ÚNICO DO SISTEMA
    private final String ADMIN_USER = "admin_aurora";
    private final String ADMIN_PASS = "aurora123";

    // =========================================================================
    // SERVIÇOS DE AUTENTICAÇÃO (LOGIN / LOGOUT DO ESTUDANTE)
    // =========================================================================

    /**
     * Get: Exibe a tela de Login do Aluno. Se já estiver logado, joga para o início.
     */
    @GetMapping("/login")
    public String telaLogin(HttpSession session) {
        if (usuarioLogado(session)) return "redirect:/inicio";
        return "Login";
    }

    /**
     * Post: Processa as credenciais enviadas pelo formulário de Login do Aluno.
     */
    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String senha, HttpSession session) {
        Usuario usuario = usuarioService.login(email, senha);
        if (usuario != null) {
            session.setAttribute("usuario", usuario); 
            return "redirect:/inicio";
        }
        return "redirect:/login?erro=true";
    }

    /**
     * Get: Invalida a sessão atual do usuário e limpa os dados salvos.
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate(); 
        return "redirect:/login";
    }

    // =========================================================================
    // GERENCIAMENTO DE CADASTRO DE NOVOS ESTUDANTES
    // =========================================================================

    /**
     * Get: Exibe o formulário de cadastro de alunos.
     */
    @GetMapping("/cadastro")
    public String telaCadastro(HttpSession session, Model model) {
        if (usuarioLogado(session)) return "redirect:/inicio";
        model.addAttribute("usuario", new Usuario());
        return "Cadastro"; 
    }

    /**
     * Post: Coleta os dados preenchidos e cria a conta do aluno se o e-mail não existir.
     */
    @PostMapping("/cadastro")
    public String realizarCadastro(@ModelAttribute Usuario usuario) {
        boolean sucesso = usuarioService.cadastrar(usuario);

        if (sucesso) {
            return "redirect:/login?sucesso=true";
        } else {
            return "redirect:/cadastro?erro=email_existente";
        }
    }

    // =========================================================================
    // CORE: PORTAL PRINCIPAL DO ESTUDANTE
    // =========================================================================

    /**
     * Get: Painel Inicial (Dashboard) do Aluno.
     * Centraliza o progresso funcional, dados de perfil, calendário e mural de avisos.
     */
    @GetMapping("/inicio")
    public String inicio(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);

        if (usuario != null) {
            model.addAttribute("cidade", usuario.getCidade());
        }
     
        // -------------------------------------------------------------
        // CÁLCULO DE PROGRESSO REAL E 100% FUNCIONAL
        // -------------------------------------------------------------
        List<Conteudo> todosConteudos = conteudoRepository.findAll();
        long totalAulas = todosConteudos != null ? todosConteudos.stream()
                .filter(c -> TipoConteudo.VIDEO.equals(c.getTipo()))
                .count() : 0;

        // Busca a contagem direta da tabela de progresso real do aluno logado
        long aulasConcluidas = progressoAulaRepository.countByUsuario(usuario);
        int porcentagemProgresso = 0;

        if (totalAulas > 0) {
            porcentagemProgresso = (int) ((aulasConcluidas * 100) / totalAulas);
        }

        model.addAttribute("progresso", porcentagemProgresso);
        model.addAttribute("concluidos", aulasConcluidas);
        model.addAttribute("totais", totalAulas);

        // Carrega os eventos do calendário acadêmico
        List<Calendario> listaEventos = calendarioRepository.findAll();
        model.addAttribute("eventos", listaEventos);

        // Carrega as publicações ativas do mural de notícias
        List<Aviso> listaAvisos = avisoRepository.findAll();
        model.addAttribute("avisos", listaAvisos);

        return "TelaInicio";
    }

    /**
     * Post: Recebe a requisição para marcar uma vídeo-aula específica como concluída.
     */
    @PostMapping("/aulas/concluir/{id}")
    public String concluirAula(@PathVariable Long id, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        Conteudo aula = conteudoRepository.findById(id).orElse(null);
        
        if (usuario != null && aula != null) {
            // Verifica se já não foi concluída antes para evitar duplicados no banco
            boolean jaConcluido = progressoAulaRepository.existsByUsuarioAndConteudo(usuario, aula);
            if (!jaConcluido) {
                ProgressoAula progresso = new ProgressoAula(usuario, aula);
                progressoAulaRepository.save(progresso);
            }
        }
        
        return "redirect:/inicio"; // Redireciona para o início para ver a barra roxa subir!
    }

    /**
     * Get: Exibe as configurações de dados cadastrais do Aluno.
     */
    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        return "PerfilAluno";
    }

    /**
     * Post: Atualiza as informações pessoais do aluno no banco de dados.
     */
    @PostMapping("/salvarPerfil")
    public String salvarPerfil(@ModelAttribute Usuario usuarioAtualizado, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        usuario.setNome(usuarioAtualizado.getNome());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setCurso(usuarioAtualizado.getCurso());
        usuario.setCidade(usuarioAtualizado.getCidade());
        usuario.setTelefone(usuarioAtualizado.getTelefone());
        usuarioService.salvar(usuario);
        session.setAttribute("usuario", usuario); // Sincroniza a sessão ativa
        return "redirect:/perfil";
    }

    /**
     * Get: Abre o Hub principal de matérias (Matemática / Português).
     */
    @GetMapping("/disciplinas")
    public String disciplinas(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        return "TelaDisciplinas";
    }

    /**
     * Get: Lista o banco de exercícios do cursinho filtrados ou gerais.
     */
    @GetMapping("/exercicios")
    public String exercicios(@RequestParam(value = "topico", required = false) String topico, HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        List<Conteudo> listaExercicios;

        if (topico != null && !topico.isEmpty()) {
            List<Conteudo> doBanco = conteudoRepository.findByNivel(topico);
            
            listaExercicios = doBanco != null ? doBanco.stream()
                .filter(c -> TipoConteudo.EXERCICIO.equals(c.getTipo()))
                .toList() : new ArrayList<>();
                
            model.addAttribute("topicoAtivo", topico);
        } else {
            List<Conteudo> todos = conteudoRepository.findByTipo(TipoConteudo.EXERCICIO);
            listaExercicios = todos != null ? todos : new ArrayList<>();
        }

        model.addAttribute("exercicios", listaExercicios);
        
        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        
        return "TelaExercicios";
    }

    /**
     * Get: Central de downloads de PDFs de Provas do ENEM anteriores.
     */
    @GetMapping("/provas")
    public String provas(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        List<Conteudo> provasAnteriores = conteudoRepository.findByTipo(TipoConteudo.PROVA);
        model.addAttribute("provas", provasAnteriores);

        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        
        return "TelaProvas";
    }

    /**
     * Get: Exibe a listagem de materiais extras em PDF do cursinho.
     */
    @GetMapping("/material")
    public String material(@RequestParam(value = "cat", required = false) String categoria, HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";

        List<Conteudo> materiaisDisponiveis = conteudoRepository.findByTipo(TipoConteudo.MATERIAL);
        
        if (categoria != null && !categoria.isEmpty() && materiaisDisponiveis != null) {
            materiaisDisponiveis = materiaisDisponiveis.stream()
                .filter(m -> categoria.equalsIgnoreCase(m.getNivel()))
                .toList();
            model.addAttribute("categoriaAtiva", categoria);
        }

        model.addAttribute("materiais", materiaisDisponiveis != null ? materiaisDisponiveis : new ArrayList<Conteudo>());

        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        
        return "TelaMaterialExtra";
    }

    // =========================================================================
    // FILTROS INDIVIDUAIS DE VIDEO-AULAS POR DISCIPLINA
    // =========================================================================

    /**
     * Get: Filtra e exibe os vídeos salvos na categoria de Matemática.
     */
    @GetMapping("/aulas/matematica")
    public String aulasMatematica(@RequestParam("topico") String topico, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";

        try {
            List<Conteudo> todasAulas = conteudoRepository.findByDisciplinaAndNivel(Disciplina.MATEMATICA, topico);
            
            List<Conteudo> aulasFiltradas = todasAulas != null ? todasAulas.stream()
                .filter(c -> TipoConteudo.VIDEO.equals(c.getTipo()))
                .toList() : new ArrayList<>();

            model.addAttribute("aulas", aulasFiltradas);
            model.addAttribute("tituloTopico", topico); 
            model.addAttribute("usuario", session.getAttribute("usuario"));
            
            return "TelaAulas";
        } catch (Exception e) {
            System.out.println("ERRO AO BUSCAR AULAS DE MATEMÁTICA: " + e.getMessage());
            return "redirect:/disciplinas";
        }
    }

    /**
     * Get: Filtra e exibe os vídeos salvos na categoria de Português.
     */
    @GetMapping("/aulas/portugues")
    public String aulasPortugues(@RequestParam("topico") String topico, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";

        try {
            List<Conteudo> todasAulas = conteudoRepository.findByDisciplinaAndNivel(Disciplina.PORTUGUES, topico);
            
            List<Conteudo> aulasFiltradas = todasAulas != null ? todasAulas.stream()
                .filter(c -> TipoConteudo.VIDEO.equals(c.getTipo()))
                .toList() : new ArrayList<>();

            model.addAttribute("aulas", aulasFiltradas);
            model.addAttribute("tituloTopico", topico);
            model.addAttribute("usuario", session.getAttribute("usuario"));
            
            return "TelaAulas";
        } catch (Exception e) {
            System.out.println("ERRO AO BUSCAR AULAS DE PORTUGUÊS: " + e.getMessage());
            return "redirect:/disciplinas";
        }
    }

    /**
     * Get: Abre o Player de reprodução do vídeo selecionado pelo aluno.
     */
    @GetMapping("/assistir/{id}")
    public String assistirAula(@PathVariable Long id, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        Conteudo aula = conteudoRepository.findById(id).orElse(null);
        if (aula == null) return "redirect:/inicio"; 

        model.addAttribute("aula", aula);
        model.addAttribute("usuario", (Usuario) session.getAttribute("usuario"));
        return "TelaAssistir"; 
    }

    // =========================================================================
    // SISTEMA ADMINISTRATIVO (ADMIN / COORDENAÇÃO)
    // =========================================================================

    /**
     * Get: Abre o login restrito da coordenação.
     */
    @GetMapping("/login-admin")
    public String loginAdmin(HttpSession session) {
        if (adminLogado(session)) return "redirect:/admin";
        return "login-admin";
    }

    /**
     * Post: Autentica o usuário administrador com dados estáticos do sistema.
     */
    @PostMapping("/admin/autenticar")
    public String autenticarAdmin(@RequestParam String usuario, @RequestParam String senha, HttpSession session, Model model) {
        if (ADMIN_USER.equals(usuario) && ADMIN_PASS.equals(senha)) {
            session.setAttribute("isAdmin", true);
            return "redirect:/admin";
        }
        model.addAttribute("erro", "Credenciais de administrador inválidas!");
        return "login-admin";
    }

    /**
     * Get: Dashboard mestre do Admin (Tela com formulário de Upload de Aulas).
     */
    @GetMapping("/admin")
    public String admin(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "admin-dashboard";
    }

    /**
     * Get: Abre a listagem global para exclusão ou conferência de aulas salvas.
     */
    @GetMapping("/admin/conteudo")
    public String gerenciarAulas(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        List<Conteudo> aulas = conteudoRepository.findAll(); 
        model.addAttribute("aulas", aulas);
        return "gerenciar-aulas";
    }

    /**
     * Get: Abre o formulário limpo para envio de vídeos.
     */
    @GetMapping("/admin/upload")
    public String telaUpload(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "upload-conteudo";
    }

    /**
     * Post: Recebe dados do formulário mestre e salva um novo vídeo no banco.
     */
    @PostMapping("/salvarconteudo")
    public String salvarConteudo(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudo.setTipo(TipoConteudo.VIDEO);
        conteudoRepository.save(conteudo); 
        return "redirect:/admin/conteudo";
    }

    /**
     * Get: Deleta um conteúdo qualquer do banco de dados usando seu ID numérico.
     */
    @GetMapping("/admin/excluir/{id}")
    public String excluirConteudo(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.deleteById(id);
        return "redirect:/admin/conteudo";
    }

    /**
     * Get: Abre a tela para o admin gerenciar/subir PDFs de Provas do ENEM.
     */
    @GetMapping("/admin/provas") 
    public String telaUploadProvas(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "upload-provas"; 
    }

    /**
     * Get: Lista e gerencia materiais de estudo extras adicionados pelo admin.
     */
    @GetMapping("/admin/materiais")
    public String gerenciarMateriaisAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        List<Conteudo> materiais = conteudoRepository.findByTipo(TipoConteudo.MATERIAL);
        model.addAttribute("materiais", materiais != null ? materiais : new ArrayList<Conteudo>());
        return "MaterialExtraAdmin"; 
    }

    /**
     * Post: Registra um novo PDF/Link de Material Extra no banco.
     */
    @PostMapping("/admin/materiais/salvar")
    public String adminSalvarMaterial(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        conteudo.setTipo(TipoConteudo.MATERIAL); 
        conteudoRepository.save(conteudo);
        return "redirect:/admin/materiais?sucesso=true";
    }

    /**
     * Get: Exclui um material extra pelo ID.
     */
    @GetMapping("/admin/materiais/excluir/{id}")
    public String adminExcluirMaterial(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        conteudoRepository.deleteById(id);
        return "redirect:/admin/materiais";
    }

    /**
     * Get: Exibe a tabela de eventos e agendamento de provas/simulados do cursinho.
     */
    @GetMapping("/admin/calendario")
    public String gerenciarCalendario(HttpSession session, Model model){
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        List<Calendario> eventos = calendarioRepository.findAll();
        model.addAttribute("eventos", eventos);
        return "gerenciar_calendario";
    }

    /**
     * Post: Grava um novo evento de data no calendário acadêmico.
     */
    @PostMapping("/admin/calendario/salvar")
    public String salvarEvento(@ModelAttribute Calendario calendario, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        calendarioRepository.save(calendario);
        return "redirect:/admin/calendario?sucesso=true";
    }
    
    /**
     * Get: Deleta um compromisso agendado do calendário.
     */
    @GetMapping("/admin/calendario/excluir/{id}")
    public String excluirEvento(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        calendarioRepository.deleteById(id);
        return "redirect:/admin/calendario";
    }

    /**
     * Get: Lista o banco de questões e exercícios postados pela coordenação.
     */
    @GetMapping("/admin/exercicios")
    public String gerenciarExerciciosAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        List<Conteudo> exercicios = conteudoRepository.findByTipo(TipoConteudo.EXERCICIO);
        model.addAttribute("exercicios", exercicios != null ? exercicios : new ArrayList<Conteudo>());
        
        return "AdminExercicios"; 
    }

    /**
     * Post: Salva um novo exercício atrelando a categoria correta automaticamente.
     */
    @PostMapping("/admin/exercicios/salvar")
    public String adminSalvarExercicio(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        conteudo.setTipo(TipoConteudo.EXERCICIO); 
        conteudoRepository.save(conteudo);
        
        return "redirect:/admin/exercicios?sucesso=true";
    }

    /**
     * Get: Remove um exercício cadastrado no sistema.
     */
    @GetMapping("/admin/exercicios/excluir/{id}")
    public String adminExcluirExercicio(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        conteudoRepository.deleteById(id);
        return "redirect:/admin/exercicios";
    }

    // =========================================================================
    // GERENCIAMENTO EXCLUSIVO DO MURAL DE AVISOS DINÂMICO
    // =========================================================================

    /**
     * 1. Get: Exibe a lista e tabela de controle com todos os avisos criados.
     */
    @GetMapping("/admin/mural")
    public String gerenciarMuralAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        model.addAttribute("avisos", avisoRepository.findAll());
        return "AdminMural"; 
    }

    /**
     * 2. Get: Abre o formulário visual com design mestre para registrar um aviso.
     */
    @GetMapping("/admin/mural/novo")
    public String abrirFormularioMural(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "admin-mural"; 
    }

    /**
     * 3. Post: Salva a nova publicação criada e redireciona para a tabela mestre.
     */
    @PostMapping("/admin/mural/salvar")
    public String salvarNovoAviso(@ModelAttribute Aviso aviso, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        avisoRepository.save(aviso); 
        return "redirect:/admin/mural"; 
    }

    /**
     * 4. Get: Limpa um aviso antigo do mural para não superlotar a Home do aluno.
     */
    @GetMapping("/admin/mural/excluir/{id}")
    public String excluirAviso(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        avisoRepository.deleteById(id);
        return "redirect:/admin/mural";
    }

    // =========================================================================
    // CHECAGENS E VERIFICAÇÕES DE SEGURANÇA INTERNAS (SESSÕES)
    // =========================================================================

    /**
     * Valida se existe um aluno autenticado na requisição.
     */
    private boolean usuarioLogado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    /**
     * Valida se o administrador master está com o token ativo na sessão.
     */
    private boolean adminLogado(HttpSession session) {
        return session.getAttribute("isAdmin") != null;
    }
}