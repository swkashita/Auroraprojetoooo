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

    // CREDENCIAIS FIXAS DO ADMINISTRADOR ÚNICO DO SISTEMA
    private final String ADMIN_USER = "admin_aurora";
    private final String ADMIN_PASS = "aurora123";

    // =========================================================================
    // TOPICO 1: SERVIÇOS DE AUTENTICAÇÃO (LOGIN / LOGOUT DO ESTUDANTE)
    // =========================================================================

    @GetMapping("/login")
    public String telaLogin(HttpSession session, Model model, @ModelAttribute("erro") String erroFlash) {
        if (usuarioLogado(session)) return "redirect:/inicio";
        
        if (model.getAttribute("erro") == null && erroFlash != null && !erroFlash.isEmpty()) {
            model.addAttribute("erro", erroFlash);
        }
        
        return "Login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String senha, HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Usuario usuario = usuarioService.login(email, senha);
        if (usuario != null) {
            session.setAttribute("usuario", usuario); 
            return "redirect:/inicio";
        }
        
        redirectAttributes.addFlashAttribute("erro", "E-mail ou senha incorretos. Verifique suas credenciais! ⚠️");
        return "redirect:/login?erro=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        session.invalidate(); 
        redirectAttributes.addFlashAttribute("sucesso", "Você saiu do portal com sucesso. Até logo! 👋");
        return "redirect:/login";
    }

    // =========================================================================
    // TOPICO 2: GERENCIAMENTO DE CADASTRO DE NOVOS ESTUDANTES
    // =========================================================================

    @GetMapping("/cadastro")
    public String telaCadastro(HttpSession session, Model model) {
        if (usuarioLogado(session)) return "redirect:/inicio";
        model.addAttribute("usuario", new Usuario());
        return "Cadastro"; 
    }

    @PostMapping("/cadastro")
    public String realizarCadastro(@ModelAttribute Usuario usuario) {
        if (usuarioService.cadastrar(usuario)) {
            return "redirect:/login?sucesso=true";
        }
        return "redirect:/cadastro?erro=email_existente";
    }

    // =========================================================================
    // TOPICO 3: PORTAL PRINCIPAL DO ESTUDANTE (DASHBOARD COM FRASES ALEATÓRIAS)
    // =========================================================================

    @GetMapping("/inicio")
    public String inicio(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        model.addAttribute("usuario", usuario);

        if (usuario != null) {
            model.addAttribute("cidade", usuario.getCidade());
            
            String cursoAlvo = usuario.getCurso();
            if (cursoAlvo == null || cursoAlvo.trim().isEmpty()) {
                model.addAttribute("mensagemMeta", "Defina seu curso dos sonhos no menu Perfil! 🎯");
            } else {
                model.addAttribute("mensagemMeta", "Foco no objetivo: " + cursoAlvo + " 🚀");
            }
        } else {
            model.addAttribute("mensagemMeta", "Estude no seu ritmo! 🎯");
        }

        String[] frasesMotivacionais = {
            "\"O success é a soma de pequenos esforços repetidos dia após dia.\"",
            "\"A disciplina é a ponte entre metas e realizações.\"",
            "\"Não estude para passar, estude até passar.\"",
            "\"Grandes jornadas começam com o primeiro passo. Mantenha o foco!\"",
            "\"Seu futuro é criado pelo que você faz hoje, não amanhã.\"",
            "\"A persistência é o caminho do êxito.\"",
            "\"Acredite que você pode e você já está no meio do caminho.\""
        };
        
        int indiceAleatorio = new java.util.Random().nextInt(frasesMotivacionais.length);
        model.addAttribute("fraseDoDia", frasesMotivacionais[indiceAleatorio]);

        model.addAttribute("eventos", calendarioRepository.findAll());
        model.addAttribute("avisos", avisoRepository.findAll());

        return "TelaInicio";
    }

    @GetMapping("/perfil")
    public String perfil(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", session.getAttribute("usuario"));
        return "PerfilAluno";
    }

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
        session.setAttribute("usuario", usuario); 
        return "redirect:/perfil";
    }

    // =========================================================================
    // TOPICO 4: ABAS DE NAVEGAÇÃO DE CONTEÚDO (MATÉRIAS, EXERCÍCIOS, PROVAS, PDF)
    // =========================================================================

    @GetMapping("/disciplinas")
    public String disciplinas(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        model.addAttribute("usuario", session.getAttribute("usuario"));
        return "TelaDisciplinas";
    }

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
        model.addAttribute("eventos", calendarioRepository.findAll());
        model.addAttribute("usuario", session.getAttribute("usuario"));
        
        return "TelaExercicios";
    }

    @GetMapping("/provas")
    public String provas(HttpSession session, Model model) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        model.addAttribute("provas", conteudoRepository.findByTipo(TipoConteudo.PROVA));
        model.addAttribute("eventos", calendarioRepository.findAll());
        model.addAttribute("usuario", session.getAttribute("usuario"));
        
        return "TelaProvas";
    }

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

        model.addAttribute("materials", materiaisDisponiveis != null ? materiaisDisponiveis : new ArrayList<>());
        model.addAttribute("eventos", calendarioRepository.findAll());
        model.addAttribute("usuario", session.getAttribute("usuario"));
        
        return "TelaMaterialExtra";
    }

    // =========================================================================
    // TOPICO 5: ENTRADA E FILTRAGEM DE VÍDEO-AULAS POR CATEGORIA
    // =========================================================================

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
            return "redirect:/disciplinas";
        }
    }

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
            return "redirect:/disciplinas";
        }
    }

    @GetMapping("/assistir/{id}")
    public String assistirAula(@PathVariable Long id, Model model, HttpSession session) {
        if (!usuarioLogado(session)) return "redirect:/login";
        
        Conteudo aula = conteudoRepository.findById(id).orElse(null);
        if (aula == null) return "redirect:/inicio"; 

        model.addAttribute("aula", aula);
        model.addAttribute("usuario", session.getAttribute("usuario"));
        return "TelaAssistir"; 
    }

    // =========================================================================
    // TOPICO 6: PORTAL DO ADMINISTRADOR (CENTRAL GERENCIAR TUDO)
    // =========================================================================

    @GetMapping("/login-admin")
    public String loginAdmin(HttpSession session) {
        if (adminLogado(session)) return "redirect:/admin";
        return "login-admin";
    }

    @PostMapping("/admin/autenticar")
    public String autenticarAdmin(@RequestParam String usuario, @RequestParam String senha, HttpSession session, Model model) {
        if (ADMIN_USER.equals(usuario) && ADMIN_PASS.equals(senha)) {
            session.setAttribute("isAdmin", true);
            return "redirect:/admin";
        }
        model.addAttribute("erro", "Credenciais de administrador inválidas!");
        return "login-admin";
    }

    // ROTA PRINCIPAL DO ADMIN (UPLOAD E DASHBOARD COM ÚLTIMAS AÇÕES)
    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        if (!model.containsAttribute("conteudo")) {
            model.addAttribute("conteudo", new Conteudo());
        }

        // Carrega os contadores e a lista de últimas ações no painel lateral
        carregarPainelLateral(model);

        return "AdminAulas"; 
    }

    // CENTRAL DE GERENCIAMENTO UNIFICADA (LISTAR TUDO)
    @GetMapping("/admin/conteudo")
    public String gerenciarTudo(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        List<Conteudo> todosConteudos = conteudoRepository.findAll();
        model.addAttribute("conteudos", todosConteudos != null ? todosConteudos : new ArrayList<>());
        
        return "gerenciartudo"; 
    }

    // ROTA PARA ACESSAR O MODO EDIÇÃO REPROVEITANDO O AdminAulas.html
    @GetMapping("/admin/editar/{id}")
    public String editarConteudoForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        Conteudo conteudo = conteudoRepository.findById(id).orElse(null);
        if (conteudo == null) return "redirect:/admin/conteudo";
        
        model.addAttribute("conteudo", conteudo);
        carregarPainelLateral(model); // Mantém o painel lateral ativo na edição
        return "AdminAulas";
    }

    // SALVAMENTO DINÂMICO INTELIGENTE
    @PostMapping("/salvarconteudo")
    public String salvarConteudo(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        if (conteudo.getTipo() == null) {
            conteudo.setTipo(TipoConteudo.VIDEO);
        }
        
        conteudoRepository.save(conteudo); 
        
        // Redireciona de volta para o painel principal para ver as Últimas Ações atualizando!
        return "redirect:/admin"; 
    }

    // EXCLUSÃO UNIFICADA
    @org.springframework.transaction.annotation.Transactional
    @GetMapping("/admin/excluir/{id}")
    public String excluirConteudo(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.deleteById(id);
        return "redirect:/admin/conteudo";
    }

    // =========================================================================
    // ROTAS ADICIONAIS DO ADMIN PARA CADASTRAR OUTROS TIPOS DE CONTEÚDO
    // =========================================================================

    // ROTA PARA UPLOAD DE EXERCÍCIOS
    @GetMapping("/admin/exercicios")
    public String adminExercicios(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        Conteudo exercicio = new Conteudo();
        exercicio.setTipo(TipoConteudo.EXERCICIO); 
        model.addAttribute("conteudo", exercicio);
        
        carregarPainelLateral(model);
        return "AdminAulas";
    }

    // ROTA PARA EXIBIR O CALENDÁRIO COM A LISTA DE DATAS CADASTRADAS
    @GetMapping("/admin/calendario")
    public String adminCalendario(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        model.addAttribute("eventos", calendarioRepository.findAll());
        return "gerenciar_calendario"; 
    }
    
    @PostMapping("/admin/calendario/salvar")
    public String salvarCalendario(@ModelAttribute Calendario calendario, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        calendarioRepository.save(calendario);
        return "redirect:/admin/calendario";
    }

    @org.springframework.transaction.annotation.Transactional
    @GetMapping("/admin/calendario/excluir/{id}")
    public String excluirCalendario(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        calendarioRepository.deleteById(id);
        return "redirect:/admin/calendario";
    }
    
    // ROTA PARA UPLOAD DE PROVAS
    @GetMapping("/admin/provas")
    public String adminProvas(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        Conteudo prova = new Conteudo();
        prova.setTipo(TipoConteudo.PROVA); 
        model.addAttribute("conteudo", prova);
        
        carregarPainelLateral(model);
        return "AdminAulas";
    }

    // ROTA PARA UPLOAD DE MATERIAIS EXTRAS
    @GetMapping("/admin/materiais")
    public String adminMateriais(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        
        Conteudo material = new Conteudo();
        material.setTipo(TipoConteudo.MATERIAL); 
        model.addAttribute("conteudo", material);
        
        carregarPainelLateral(model);
        return "AdminAulas";
    }

    // =========================================================================
    // TOPICO 7: PUBLICAÇÃO E MANUTENÇÃO DO MURAL DE AVISOS DINÂMICO
    // =========================================================================

    @GetMapping("/admin/mural")
    public String gerenciarMuralAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        model.addAttribute("avisos", avisoRepository.findAll());
        return "AdminMural"; 
    }

    @GetMapping("/admin/mural/novo")
    public String abrirFormularioMural(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "AdminMural"; 
    } 
    
    @PostMapping("/admin/mural/salvar")
    public String salvarNovoAviso(@ModelAttribute Aviso aviso, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        avisoRepository.save(aviso); 
        return "redirect:/admin/mural"; 
    }

    @GetMapping("/admin/mural/excluir/{id}")
    public String excluirAviso(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        avisoRepository.deleteById(id);
        return "redirect:/admin/mural";
    }

    // =========================================================================
    // TOPICO 8: CHECAGENS AUXILIARES DE SESSÃO E MÉTODOS PRIVADOS REUTILIZÁVEIS
    // =========================================================================

    // FUNÇÃO PRIVADA QUE ATUALIZA O FEED DE ÚLTIMAS AÇÕES AUTOMATICAMENTE
   // FUNÇÃO PRIVADA QUE ATUALIZA O FEED DE ÚLTIMAS AÇÕES AUTOMATICAMENTE
    private void carregarPainelLateral(Model model) {
        try {
            List<Conteudo> todos = conteudoRepository.findAll();
            
            if (todos == null) {
                todos = new ArrayList<>();
            }

            // Conta os vídeos
            long qtdVideos = todos.stream()
                .filter(c -> c.getTipo() != null && TipoConteudo.VIDEO.equals(c.getTipo()))
                .count();
                
            // Conta as provas    
            long qtdProvas = todos.stream()
                .filter(c -> c.getTipo() != null && TipoConteudo.PROVA.equals(c.getTipo()))
                .count();

            // NOVA CONTAGEM: Conta os exercícios
            long qtdExercicios = todos.stream()
                .filter(c -> c.getTipo() != null && TipoConteudo.EXERCICIO.equals(c.getTipo()))
                .count();
            
            model.addAttribute("qtdVideos", qtdVideos);
            model.addAttribute("qtdProvas", qtdProvas);
            model.addAttribute("qtdExercicios", qtdExercicios); // Enviando para o HTML

            // Lista de últimas ações
            List<Conteudo> ultimasAcoes = todos.stream()
                .filter(c -> c.getId() != null)
                .sorted((c1, c2) -> c2.getId().compareTo(c1.getId()))
                .limit(4)
                .toList();

            model.addAttribute("ultimasAcoes", ultimasAcoes);
            
        } catch (Exception e) {
            model.addAttribute("qtdVideos", 0);
            model.addAttribute("qtdProvas", 0);
            model.addAttribute("qtdExercicios", 0);
            model.addAttribute("ultimasAcoes", new ArrayList<Conteudo>());
        }
    }

    private boolean usuarioLogado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    private boolean adminLogado(HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        return isAdmin != null && isAdmin;
    }
}