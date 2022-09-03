package ds.pirate.backend.service.ArticleService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import ds.pirate.backend.dto.ArticleDTO;
import ds.pirate.backend.dto.acommentDTO;
import ds.pirate.backend.dto.acommentRateDTO;
import ds.pirate.backend.dto.reportDTO;
import ds.pirate.backend.entity.ArticlesList;
import ds.pirate.backend.entity.HashTags;
import ds.pirate.backend.entity.ImagesList;
import ds.pirate.backend.entity.acommentRate;
import ds.pirate.backend.entity.acomments;
import ds.pirate.backend.entity.airUser;
import ds.pirate.backend.entity.reportList;
import ds.pirate.backend.repository.ArticleReportRepository;
import ds.pirate.backend.repository.ArticleRepository;
import ds.pirate.backend.repository.CommentRateRepository;
import ds.pirate.backend.repository.CommentRepository;
import ds.pirate.backend.repository.HashTagRepository;
import ds.pirate.backend.repository.ImageRepository;
import ds.pirate.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ArticleServiceImpl implements ArticleService{
    private final ArticleRepository repo;
    private final HashTagRepository hrepo;
    private final ImageRepository irepo;
    private final CommentRepository crepo;
    private final CommentRateRepository ctrepo;
    private final UserRepository urepo;
    private final ArticleReportRepository arepo;


@Override
public String addArticleReport(reportDTO dto) {
    Optional<reportList> chekcing = arepo.checkReportLogByUserIdAndArticleId(dto.getUserid(), dto.getArticleid());
    if(chekcing.isPresent()){
        return "이미 신고한 글입니다";
    }else{
        arepo.save(reportDTOtoEntity(dto));
        return "신고가 완료되었습니다";
    }
}


    @Override
    public Double getArticleAvgRating(Long aid) { 
        return crepo.getAvgByAid(aid);
    }
    @Override
    public String rateupComment(acommentRateDTO dto) {
        acomments result = crepo.getCommentByCidAndUserid(dto.getCid(), dto.getUserid()).get();
        acommentDTO cdto = commentEntityToDTO(result);
        
        Optional<acommentRate> isRated = ctrepo.getIsRatedByCidAndUserid(dto.getCid(), dto.getUserid());
        if(isRated.isPresent()){
            cdto.setRate(cdto.getRate()-isRated.get().getUpdown());
            ctrepo.delete(isRated.get());
            crepo.save(commentDTOtoEntity(cdto));
            return "취소되었습니다";
        }else{
            cdto.setRate(cdto.getRate()+dto.getUpdown());
            crepo.save(commentDTOtoEntity(cdto));
            ctrepo.save(acommentRate.builder().commentid(cdto.getCid()).userid(cdto.getUserid()).updown(dto.getUpdown()).build());
            log.info("없음 등록 ㄲ");
            return "등록되었습니다";
        }
    }

    @Override
    public Long addNewComment(acommentDTO dto) {
        Optional<airUser> result = urepo.findByEmail(dto.getEmail());
        dto.setCommentGroup(dto.getCommentGroup()+1);
        dto.setCommnetDepth(0L);
        dto.setCommentSorts(0L);
        dto.setUserid(result.get().getUserid());
        dto.setRate(0);
        
        
        acomments entity = commentDTOtoEntity(dto);
        
        crepo.save(entity);
        return entity.getCid();
    }

    @Override
    public Long addNewCommentReply(acommentDTO dto) {
        Optional<airUser> result = urepo.findByEmail(dto.getEmail());
        dto.setCommentGroup(dto.getCommentGroup());
        dto.setCommnetDepth(dto.getCommnetDepth());
        dto.setCommentSorts(dto.getCommentSorts());
        dto.setUserid(result.get().getUserid());
        dto.setRate(0);
        
        
        acomments entity = commentDTOtoEntity(dto);
        
        crepo.save(entity);
        return entity.getCid();
    }

    @Override
    public List<acommentDTO> getCommentListByAid(Long aid) {
        Optional<List<acomments>> entity = crepo.getListByAid(aid);
        
        if (entity.isPresent()){
            List<acommentDTO> dto = entity.get()
                                            .stream()
                                            .map(cmt -> commentEntityToDTO(cmt))
                                            .collect(Collectors.toList());
            return dto;
        }
        return null;
    }

    @Override
    public ArticleDTO getArticleInfoByAid(Long aid) {
        ArticlesList result = repo.getByAid(aid);
        ArticleDTO dto = EntityToDTO(result);
        List<String> hashString =  hrepo.getList(result.getAid())
                                        .stream()
                                        .map(hentity -> hentity.getHashTagName())
                                        .collect(Collectors.toList());
        dto.setTags(hashString);
        return dto;
    }

    @Override
    public String addArticle(ArticleDTO dto, List<String> tags) {
        ArticlesList result = dtoToEntity(dto);
        repo.save(result);

        List<ImagesList> lists = dto.getImages();
        lists.forEach(i->{
            irepo.save(ImagesList.builder().fileName(i.getFileName()).articles(result).build());
        });

        for(String i : tags){
            HashTags hresult = listToHSEntity(i);
            hresult.updateArticles(result);
            hrepo.save(hresult);
        }



        
        return result.getAid().toString();
    }
}

