package br.com.totvs.desafio_totvs.service;

import br.com.totvs.desafio_totvs.dto.ContaDTO;
import br.com.totvs.desafio_totvs.dto.ContaReportDTO;
import br.com.totvs.desafio_totvs.helper.CSVHelper;
import br.com.totvs.desafio_totvs.model.Conta;
import br.com.totvs.desafio_totvs.model.SituacaoConta;
import br.com.totvs.desafio_totvs.repository.ContaRepository;
import br.com.totvs.desafio_totvs.repository.SituacaoContaRepository;
import br.com.totvs.desafio_totvs.specification.ContaSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContaService
{
    private final ContaRepository contaRepository;
    private final SituacaoContaRepository situacaoContaRepository;

    public void delete(long contaId)
    {
        Optional<Conta> conta = contaRepository.findById(contaId);
        conta.ifPresent(contaRepository::delete);
    }

    public List<ContaDTO> getContasByFilters(LocalDate vencimentoDate, String descricao) {
        Specification<Conta> spec = Specification.where(ContaSpecification.hasVencimentoDate(vencimentoDate));

        if (descricao != null && !descricao.isEmpty()) {
            spec = spec.and(ContaSpecification.descricaoContains(descricao));
        }

        List<Conta> contas = contaRepository.findAll(spec);
        return contas.stream().map(ContaDTO::convert).collect(Collectors.toList());
    }

    public List<ContaDTO> saveByCSV(MultipartFile file)
    {
        try
        {
            List<Conta> contas = CSVHelper.csvToTutorials(file.getInputStream());
            contaRepository.saveAll(contas);
            return contas.stream().map(ContaDTO::convert).collect(Collectors.toList());
        } catch (IOException e)
        {
            throw new RuntimeException("Erro ao salvar arquivo csv: " + e.getMessage());
        }
    }


    /*public List<ContaDTO> getContasByFilter(LocalDate dataVencimento, String descricao)
    {
        List<Conta> contas = contaRepository.getContasByFilters(dataVencimento, descricao);
        return contas.stream().map(ContaDTO::convert).collect(Collectors.toList());
    }*/

    public ContaReportDTO getTotalByDateRange(LocalDate startDate, LocalDate endDate) {
        Specification<Conta> spec = ContaSpecification.hasDateBetween(startDate, endDate);
        List<Conta> contas = contaRepository.findAll(spec);
        BigDecimal result = contas.stream()
                .map(Conta::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ContaReportDTO contaReportDTO = new ContaReportDTO();
        contaReportDTO.setTotal(result);
        return contaReportDTO;
    }

    /*public ContaReportDTO getContaTotalByDateAntigo(LocalDate dataInicio, LocalDate dataFim)
    {
        return contaRepository.getContaTotalByDate(dataInicio, dataFim);
    }*/

    public Page<ContaDTO> getAll(Pageable page)
    {
        Page<Conta> contas = contaRepository.findAll(page);
        return contas.map(ContaDTO::convert);
    }

    public ContaDTO save(ContaDTO contaDTO)
    {
        contaDTO.setDataCadastro(LocalDateTime.now());
        Conta conta = contaRepository.save(Conta.convert(contaDTO));
        return  ContaDTO.convert(conta);
    }

    public Page<ContaDTO> getContasBySituacaoContaId(short situacaoContaId, Pageable page)
    {
        Page<Conta> contas = contaRepository.getContasBySituacaoConta(situacaoContaId, page);
        return contas.map(ContaDTO::convert);
    }

    public ContaDTO findContaById(long id)
    {
        Optional<Conta> conta = contaRepository.findById(id);
        return conta.map(ContaDTO::convert).orElse(null);
    }

    public  ContaDTO editSituacaoConta(long idConta, short idSituacao)
    {
        Conta conta = contaRepository.findById(idConta).orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        SituacaoConta situacaoConta = situacaoContaRepository.findById(idSituacao).orElseThrow(() -> new RuntimeException("Id de Categoria não existente"));
        conta.setSituacaoConta(situacaoConta);
        return ContaDTO.convert(contaRepository.save(conta));
    }

    public ContaDTO editConta(long id, ContaDTO contaDTO)
    {
        Conta conta = contaRepository.findById(id).orElseThrow(() -> new RuntimeException("Conta não encontrada"));
        if ( contaDTO.getDataPagamento() != null )
        {
            conta.setDataPagamento(contaDTO.getDataPagamento());
        }
        if ( contaDTO.getDataVencimento() != null )
        {
            conta.setDataVencimento(contaDTO.getDataVencimento());
        }
        if (contaDTO.getSituacaoConta() != null && (contaDTO.getSituacaoConta().getId() >= 1 && contaDTO.getSituacaoConta().getId() <= 3))
        {
            conta.setSituacaoConta(SituacaoConta.convert(contaDTO.getSituacaoConta()));
        }
        if ( contaDTO.getValor() != null)
        {
            conta.setValor(contaDTO.getValor());
        }
        if ( contaDTO.getDescricao() != null && !contaDTO.getDescricao().isBlank() )
        {
            conta.setDescricao(contaDTO.getDescricao());
        }
        return ContaDTO.convert(contaRepository.save(conta));
    }

}
