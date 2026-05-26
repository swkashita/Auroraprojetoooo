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
        // Se o usuário já estiver logado, manda para o início
        if (usuarioLogado(session)) return "redirect:/inicio";
        
        // Se o erro veio via Flash Attribute (do redirect do PostMapping), ele já injeta no modelo automaticamente.
        // Mas se por acaso não veio e você quiser tratar parâmetros da URL (?erro=true), fazemos esta checagem:
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
        
        // Define o Flash Attribute para o redirecionamento
        redirectAttributes.addFlashAttribute("erro", "E-mail ou senha incorretos. Verifique suas credenciais! ⚠️");
     return "redirect:/login?erro=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        session.invalidate(); // Destrói a sessão e limpa a memória do servidor
        
        // Envia a mensagem amigável de sucesso para a tela de login
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
            
            // LÓGICA DO PAINEL "MINHA META"
            String cursoAlvo = usuario.getCurso();
            if (cursoAlvo == null || cursoAlvo.trim().isEmpty()) {
                model.addAttribute("mensagemMeta", "Defina seu curso dos sonhos no menu Perfil! 🎯");
            } else {
                model.addAttribute("mensagemMeta", "Foco no objetivo: " + cursoAlvo + " 🚀");
            }
        } else {
            model.addAttribute("mensagemMeta", "Estude no seu ritmo! 🎯");
        }

        // --- LISTA DE FRASES VARIADAS PARA O RODAPÉ DO CARD ---
        String[] frasesMotivacionais = {
            "\"O sucesso é a soma de pequenos esforços repetidos dia após dia.\"",
            "\"A disciplina é a ponte entre metas e realizações.\"",
            "\"Não estude para passar, estude até passar.\"",
            "\"Grandes jornadas começam com o primeiro passo. Mantenha o foco!\"",
            "\"Seu futuro é criado pelo que você faz hoje, não amanhã.\"",
            "\"A persistência é o caminho do êxito.\"",
            "\"Acredite que você pode e você já está no meio do caminho.\""
        };
        
        // Sorteia um índice aleatório da lista
        int indiceAleatorio = new java.util.Random().nextInt(frasesMotivacionais.length);
        model.addAttribute("fraseDoDia", frasesMotivacionais[indiceAleatorio]);
        // -----------------------------------------------------

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
    // TOPICO 6: PORTAL DO ADMINISTRADOR (UPLOAD DE COMPROMISSOS, PDFs E AULAS)
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

    @GetMapping("/admin")
    public String admin(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "admin-dashboard";
    }

    @GetMapping("/admin/conteudo")
    public String gerenciarAulas(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        model.addAttribute("aulas", conteudoRepository.findAll());
        return "gerenciar-aulas";
    }

    @GetMapping("/admin/upload")
    public String telaUpload(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "upload-conteudo";
    }

    @PostMapping("/salvarconteudo")
    public String salvarConteudo(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudo.setTipo(TipoConteudo.VIDEO);
        conteudoRepository.save(conteudo); 
        return "redirect:/admin/conteudo";
    }

    @GetMapping("/admin/excluir/{id}")
    public String excluirConteudo(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.deleteById(id);
        return "redirect:/admin/conteudo";
    }

    @GetMapping("/admin/provas") 
    public String telaUploadProvas(HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        return "upload-provas"; 
    }

    @GetMapping("/admin/materiais")
    public String gerenciarMaterialsAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        List<Conteudo> materiais = conteudoRepository.findByTipo(TipoConteudo.MATERIAL);
        model.addAttribute("materiais", materiais != null ? materiais : new ArrayList<>());
        return "MaterialExtraAdmin"; 
    }

    @PostMapping("/admin/materiais/salvar")
    public String adminSalvarMaterial(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudo.setTipo(TipoConteudo.MATERIAL); 
        conteudoRepository.save(conteudo);
        return "redirect:/admin/materiais?sucesso=true";
    }

    @GetMapping("/admin/materiais/excluir/{id}")
    public String adminExcluirMaterial(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.deleteById(id);
        return "redirect:/admin/materiais";
    }

    @GetMapping("/admin/calendario")
    public String gerenciarCalendario(HttpSession session, Model model){
        if (!adminLogado(session)) return "redirect:/login-admin";
        model.addAttribute("eventos", calendarioRepository.findAll());
        return "gerenciar_calendario";
    }

    @PostMapping("/admin/calendario/salvar")
    public String salvarEvento(@ModelAttribute Calendario calendario, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        calendarioRepository.save(calendario);
        return "redirect:/admin/calendario?sucesso=true";
    }
    
    @GetMapping("/admin/calendario/excluir/{id}")
    public String excluirEvento(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        calendarioRepository.deleteById(id);
        return "redirect:/admin/calendario";
    }

    @GetMapping("/admin/exercicios")
    public String gerenciarExerciciosAdmin(HttpSession session, Model model) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        List<Conteudo> exercicios = conteudoRepository.findByTipo(TipoConteudo.EXERCICIO);
        model.addAttribute("exercicios", exercicios != null ? exercicios : new ArrayList<>());
        return "AdminExercicios"; 
    }

    @PostMapping("/admin/exercicios/salvar")
    public String adminSalvarExercicio(@ModelAttribute Conteudo conteudo, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudo.setTipo(TipoConteudo.EXERCICIO); 
        conteudoRepository.save(conteudo);
        return "redirect:/admin/exercicios?sucesso=true";
    }

    @GetMapping("/admin/exercicios/excluir/{id}")
    public String adminExcluirExercicio(@PathVariable Long id, HttpSession session) {
        if (!adminLogado(session)) return "redirect:/login-admin";
        conteudoRepository.deleteById(id);
        return "redirect:/admin/exercicios";
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
    // TOPICO 8: CHECAGENS AUXILIARES DE SESSÃO (MÉTODOS PRIVADOS)
    // =========================================================================

    private boolean usuarioLogado(HttpSession session) {
        return session.getAttribute("usuario") != null;
    }

    private boolean adminLogado(HttpSession session) {
        return session.getAttribute("isAdmin") != null;
    }
}