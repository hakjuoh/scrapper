package oh.hakju.scrapper;

import oh.hakju.scrapper.dao.ArticleDAO;
import oh.hakju.scrapper.dao.SentenceDAO;
import oh.hakju.scrapper.entity.Article;
import oh.hakju.scrapper.entity.Sentence;
import opennlp.tools.sentdetect.*;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleToSentence implements Runnable, Closeable {

    private SentenceModel sentenceModel;

    private ArticleDAO articleDAO = new ArticleDAO();

    private SentenceDAO sentenceDAO = new SentenceDAO();

    private ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 1);

    public ArticleToSentence(SentenceModel sentenceModel) {
        this.sentenceModel = sentenceModel;
    }

    @Override
    public void run() {
        Long minArticleId = sentenceDAO.findMinArticleId();
        Long maxArticleId = articleDAO.findMaxArticleId();

        if (minArticleId == maxArticleId) {
            return;
        }

        for (Long i = minArticleId; i < maxArticleId; i++) {
            execute(i);
        }
    }

    public void execute(Long articleId) {
        Worker worker = new Worker(articleId);
        executor.execute(worker);
    }

    @Override
    public void close() throws IOException {
        executor.shutdown();
    }

    private class Worker implements Runnable {
        private Long articleId;

        public Worker(Long articleId) {
            this.articleId = articleId;
        }

        @Override
        public void run() {
            Article article = articleDAO.findById(articleId);
            if (article == null) {
                return;
            }

            String text = article.getText();
            int paragraphSeq = 1;
            for (String paragraph : getParagraphs(text)) {
                int sentenceSeq = 1;
                for (String sentence : getSentences(paragraph)) {

                    Sentence entity = new Sentence();
                    entity.setArticleId(articleId);
                    entity.setParagraphSeq(paragraphSeq++);
                    entity.setSentenceSeq(sentenceSeq++);
                    entity.setText(sentence);

                    sentenceDAO.insert(entity);
                    System.out.println("Inserted the following sentence: " + sentence);
                }
            }
        }

        private List<String> getParagraphs(String text) {
            List<String> paragraphs = new ArrayList();
            StringTokenizer paragraphTokenizer = new StringTokenizer(text, "\n");
            while (paragraphTokenizer.hasMoreTokens()) {
                String paragraph = paragraphTokenizer.nextToken();
                paragraphs.add(paragraph);
            }
            return paragraphs;
        }

        private List<Character> END_CHAR = Arrays.asList('.', '!', '?');
        private List<String> PREFIXS_OF_PERSON_NAME = Arrays.asList("Mr", "Ms", "Mrs");

        private List<String> getSentences(String paragraph) {
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);
            return Arrays.asList(sentenceDetector.sentDetect(paragraph));
        }

        private List<Character> STOP_CHAR = Arrays.asList('\"', '(', ')');

        private String normalize(String token) {
            StringBuilder sb = new StringBuilder();
            for (char ch : token.toCharArray()) {
                if (STOP_CHAR.contains(ch)) {
                    continue;
                }

                sb.append(ch);
            }

            return sb.toString();
        }
    }

    public static void main(String[] args) throws IOException {
        
        
        
        String trainingStr = "Last September, as letters editor of The Times, I used some of this space for an essay called \"To the Reader,\" introducing myself and outlining the mission and the mechanics of the letters page.\n" +
                "It seemed to strike a chord, and scores of readers wrote back.\n" +
                "Many were pleased to learn that the anonymous editor had a name.\n" +
                "Some were grateful for the advice; others were amused, acerbic, occasionally even dyspeptic.\n" +
                "I had my 15 minutes of fame: a flurry of dissection on the Internet; an interview on TV that lasted, well, about 15 minutes. We printed two letters in response — pro and con, naturally.\n" +
                "But readers, new and old, send in questions (and even complaints!) about the letters page almost every day, and so a refresher course may help.\n" +
                "This is an attempt to answer some frequently asked questions.\n" +
                "I've submitted many letters, but none have been published.\n" +
                "How can I improve my chances?\n" +
                "Thanks largely to the ease and ubiquity of e-mail, letters submissions (and a lot besides) come in relentlessly, round the clock, from around the country and around the world, at a rate of roughly a thousand a day.\n" +
                "My small staff and I try to read them all, but we can publish only about 15 letters a day.\n" +
                "While the odds are long, some letter writers seem to know how to shorten them.\n" +
                "Here are some tips: Write quickly, concisely and engagingly.\n" +
                "We're in an age of fast-moving news and virtually instant reaction; letters about an especially timely topic often appear within a day or two (and almost always within a week).\n" +
                "At times, some big stories generate hundreds of letters a day — Sept. 11 (at one point we were getting hundreds an hour), the war in Iraq, politics, to name a few.\n" +
                "When you write about a particularly contentious issue, bear in mind that many others do so as well.\n" +
                "We can try to capture a sense of what's on readers' minds, but we can't be comprehensive.\n" +
                "Your suggested length for letters is about 150 words.\n" +
                "Why so short?\n" +
                "(Or, as one writer put it after I cited the brevity of the Gettysburg Address, \"Why does Lincoln get 250 and the rest of us a measly 150?\")\n" +
                "Ideally, the letters page should be a forum for a variety of voices, and that means letting a lot of readers have a turn.\n" +
                "With our limited space, we have room for letters that make their case with a point or two, but not for full-length articles.\n" +
                "(For those, try our neighbors at the Op-Ed page.)\n" +
                "Once in a while, a particularly eloquent, newsworthy or pointed letter is allotted Lincolnesque space in print, but that is the exception.\n" +
                "You've said that the letters page \"does not have a political coloration of its own.\"\n" +
                "Yet liberal opinion seems to dominate, and conservatives seem to have a lesser voice.\n" +
                "Why?\n" +
                "In selecting letters, I try to present a fair sampling of reader opinion, as well as a balance of views, pro and con.\n" +
                "Writers to The Times — by no means all, certainly, but a clear majority — tend to be liberal, often vociferously so.\n" +
                "Among our letter writers, critics of the Bush administration, especially over the war in Iraq, outnumber its defenders by a substantial margin.\n" +
                "On same-sex marriage, to cite another example, proponents far outnumber opponents among our letter writers.\n" +
                "But there is more of a divide on other national issues, like abortion, affirmative action and immigration.\n" +
                "We welcome opinions from all sides: the majority, the dissenters, the contrarians.\n" +
                "While I naturally have to use my judgment, it's not my opinion that determines the complexion of the page, it's yours.\n" +
                "Do you edit letters?\n" +
                "We reserve the right to edit for space, clarity, civility and accuracy, and we send you the edited version before publication.\n" +
                "If your letter is selected, we will try to reach you and ask a few questions: Did you write the letter?\n" +
                "(We're not amused by impostors.)\n" +
                "Is it exclusive to The Times?\n" +
                "(It should be.)\n" +
                "Do you have a connection to the subject you're writing about?\n" +
                "(Readers should be able to judge your credibility and motivation.)\n" +
                "What is your responsibility for ensuring that facts cited in letters are accurate?\n" +
                "Letter writers, to use a well-worn phrase, are entitled to their own opinions, but not to their own facts.\n" +
                "There is, of course, a broad gray area in which hard fact and heartfelt opinion commingle.\n" +
                "But we do try to verify the facts, either checking them ourselves or asking writers for sources of information.\n" +
                "Sometimes we goof, and then we publish corrections.\n" +
                "Why are there so many letters from people with credentials or titles after their names?\n" +
                "These come in many flavors: an official's response to criticism; a statement of policy, printed for the record or for its news value; a view that we feel adds an interesting perspective or expertise to the debate.\n" +
                "As with any letter, writers speak only for themselves or their organizations; publication should not be taken as an endorsement of that view by The Times.\n" +
                "The aim is to stimulate discussion, not end it.\n" +
                "A personal note, for those who've asked: I've been an editor at The Times for 23 years and counting, nearly 5 as letters editor, and a New Yorker since early childhood.\n" +
                "I was born in Budapest, Hungary, in 1953 and came to America with my parents — survivors of Nazism and refugees from Communism — in 1957.\n" +
                "Five years later, we swore an oath as naturalized American citizens.\n" +
                "Perhaps unsurprisingly, then, my core belief as letters editor is that healthy, informed debate is the lifeblood of a strong democracy.\n" +
                "Other than that, I'm an avid Times reader, just like you.\n" +
                "If what's in this newspaper interests you, it interests me.\n" +
                "“I’m a little embarrassed by the state of this room,” said Will Shortz, The New York Times’s crossword editor, as he waded through a seemingly endless array of puzzle ephemera in his upstairs library.\n" +
                "“The problem is, I recently lent part of my collection for use at an exhibition.\n" +
                "I got everything back, but I haven’t put it all away yet.”\n" +
                "He paused, looking up from a stack of old magazines.\n" +
                "“Well, it’s more than that, of course,” he said.\n" +
                "“Things are just — piling up.”\n" +
                "Indeed they are.\n" +
                "Mr. Shortz’s collection includes more than 25,000 puzzle books and magazines, dating to 1534, along with pamphlets, small mechanical puzzles and other ephemeral items.\n" +
                "It overwhelms the décor of his home in Pleasantville, N.Y., where he lives and works.\n" +
                "A clock in his office is — well, its face is a crossword puzzle.\n" +
                "(The hands? Two stubby pencils.)\n" +
                "A display case in the living room holds, among other treasures, the first crossword puzzle ever published — in a 1913 Sunday “Fun” section of The New York World.\n" +
                "Even the tiled floor in the upstairs bathroom, made of small black and white squares, calls to mind a crossword grid.\n" +
                "The collection is the embodiment of a lifelong obsession with puzzles.\n" +
                "In fact, aside from a law degree earned at least in part to mollify his parents — which he undertook after completing a self-designed undergraduate degree in enigmatology — Mr. Shortz has spent his professional life pursuing little else.\n" +
                "And that law degree, by the way?\n" +
                "“Everyone else took trial advocacy, but I didn’t have to; I knew I was never going to argue a case in court,” he said with a grin.\n" +
                "“I took two courses on intellectual property.\n" +
                "For one, I wrote a paper on copyright protection for puzzles and games.”\n" +
                "Even among his colleagues, Mr. Shortz’s singular obsession is something of a rarity: At The Times, journalistic expertise is usually not tied to a single beat.\n" +
                "An editor on the national desk, for example, may very well leap to the food desk; after a stint covering the markets, a business reporter might ship overseas to cover international news from Hong Kong.\n" +
                "Mr. Shortz’s career, though, has followed a less circuitous path.\n" +
                "“When I was a kid, I imagined a life where I’d be sitting in an attic somewhere, making my little puzzles for $15 each, somehow surviving,” he said.\n" +
                "“I actually wrote a paper in eighth grade about what I wanted to do with my life, and it was to be a professional puzzle maker.”\n" +
                "(The paper earned him a B+.\n" +
                "“I thought you would connect this to the topic of becoming an adult,” his teacher wrote.)\n" +
                "After a brief postgraduate stint at Penny Press and a 15-year run at Games magazine, Mr. Shortz joined The Times as crossword editor in 1993.\n" +
                "His aim, from the start, was to draw in a younger and more diverse audience.\n" +
                "“When I began at The Times, everyone had an opinion about my style of editing,” he said.\n" +
                "“I was 36 years younger than my predecessor, and so, to me, things were kind of old and stuffy.\n" +
                "I wanted to be a little more modern, to embrace everybody.”\n" +
                "(As a general rule, anything that appears elsewhere in The Times — from operatic trivia to the latest in pop culture — is fair game in the crossword.)\n" +
                "Mr. Shortz spends his working hours selecting and editing the puzzles that, nowadays, appear in a variety of genres: in print, on nytimes.com and on The Times’s iOS and Android apps.\n" +
                "He’s assisted by Joel Fagliano, the digital puzzles editor, who also constructs the mini crosswords that appear every day on the New York Times app and in print on Page A3.\n" +
                "Evaluating puzzle submissions can be tricky, since even minor errors in the grid can be difficult to fix without edits spiderwebbing outward.\n" +
                "More often than not, if a grid proves problematic, the puzzle is rejected.\n" +
                "It’s relatively common, however, for Mr. Shortz and Mr. Fagliano to extensively rework a given puzzle’s clues.\n" +
                "“The bad puzzles, we can tell within 10 or 15 seconds,” Mr. Shortz said.\n" +
                "“The really good ones — you can tell in a minute or two if it’s great.\n" +
                "It’s the ones in the middle that take the most time.”\n" +
                "Everyone who submits a puzzle gets a reply, yes or no.\n" +
                "“And we try to say something about the puzzle, too — what we like about it, or what we don’t like,” Mr. Shortz explained.\n" +
                "“It could just be as simple as: The theme didn’t excite us enough.”\n" +
                "Around 75 crossword submissions land on Mr. Shortz’s desk every week.\n" +
                "Each is read at least twice — once by Mr. Shortz, and once by either Mr. Fagliano or a dedicated puzzles intern.\n" +
                "After a puzzle is accepted, it’s assigned a day of the week (the puzzles increase in difficulty as the week progresses) and is slated for publication.\n" +
                "“Mondays should be all — or virtually all — familiar vocabulary, and a straightforward theme,” Mr. Shortz explained.\n" +
                "“That doesn’t mean it can’t be sophisticated, but it has to be straightforward.”\n" +
                "“Tuesday and Wednesday, the vocabulary can be a little harder, and the themes can be a little more playful — with puns and other sorts of wordplay.\n" +
                "Thursday is our hardest theme day.\n" +
                "We’ll do tricks, some serious tricks, or some more advanced wordplay.\n" +
                "Friday and Saturday puzzles tend to be themeless.”\n" +
                "And Sunday, of course, is the larger grid — 21 by 21.\n" +
                "The general quality of crossword puzzles, Mr. Shortz contends, has markedly improved in the last few decades.\n" +
                "“It used to be that, for constructors, the only feedback they got on a puzzle was from the editor,” he explained.\n" +
                "Nowadays, with all the online crossword communities — there are at least four daily blogs about the New York Times crossword — aspiring puzzle makers can engage more easily with their peers.\n" +
                "Technologies have changed, too.\n" +
                "“It used to be if you were making crosswords, you’d have a dictionary, a thesaurus, an almanac and maybe a word list,” he said.\n" +
                "“Now there are programs and databases that help make crosswords better — with better vocabulary and better theme examples.”\n" +
                "Mr. Shortz indulges his fascination with puzzle in others ways, too — by hosting the puzzles segment every Sunday on NPR, and by serving as program director for the National Puzzlers’ League convention.\n" +
                "He is also the founder and director of the American Crossword Puzzle Tournament.\n" +
                "And, in addition to puzzles, he has another great love: table tennis.\n" +
                "With a friend, Robert Roberts, Mr. Shortz opened the Westchester Table Tennis Center in 2011.\n" +
                "The club, which Mr. Shortz owns and Mr. Roberts manages, has more than 150 members and has hosted a range of events, from the North American Championships to a weekly series, held on Wednesday evenings, for players with Parkinson’s disease.\n" +
                "Through his championing of puzzles — and with public appearances on programs like “The Oprah Winfrey Show” and “The Simpsons” — Mr. Shortz has earned admiring fans the world over.\n" +
                "In fact, outside his office hangs a framed letter from former President Bill Clinton, sent in 2002, in celebration of Mr. Shortz’s 50th birthday.\n" +
                "After alluding to the Cherokees — who, Mr. Clinton writes, “believed a man didn’t reach full maturity until 51” — the letter concludes with a friendly presidential dictate.\n" +
                "“Keep the crosswords coming,” it reads.\n" +
                "“Even when I can’t finish them, they’re the only part of The Times that guarantees good feeling!”\n" +
                "Ultimately, though, his celebrity seems to have had little impact on a fascination, and a career, that took root remarkably early in life.\n" +
                "Whether discussing the world’s first crossword book (published by Simon & Schuster, in 1924, which started their company), or reminiscing about the creative influence of his mother (Wilma, who wrote children’s stories and articles about horses), or showing off a century-old magazine full of Sam Loyd puzzles (his “childhood hero”), Mr. Shortz, who is 64, exudes an infectious, boyish enthusiasm that registers as uncannily genuine.\n" +
                "“I’m very lucky,” he said, when asked to reflect on the course of his career.\n" +
                "“I tell people: If you can, figure out what you love to do the most — and then see if you can make a living doing it.”\n" +
                "Noura Jackson called 911 at 5 a.m. on Sunday, June 5, 2005.\n" +
                "‘‘Please, I need, I need an ambulance, I need an ambulance right now!’’ she cried.\n" +
                "‘‘Someone broke into my house. My mom — my mom is bleeding.’’\n" +
                "She panted as she waited a few long seconds for the operator to transfer her.\n" +
                "‘‘She’s not breathing,’’ Noura said, sounding desperate, when an emergency dispatcher came on the line.\n" +
                "‘‘She’s not breathing.\n" +
                "She’s not breathing.\n" +
                "Please help me.\n" +
                "There’s blood everywhere!’’\n" +
                "When the police arrived, Jennifer Jackson’s body lay on her bedroom floor in the brick home she owned in a well-kept Memphis neighborhood.\n" +
                "Noura’s mother, a 39-year-old successful investment banker, had been stabbed 50 times.\n" +
                "The brutal violence on a quiet block made local headlines, generating shock and anxiety in the middle-\u00ADclass corners of the city.\n" +
                "The police began their investigation with few leads.\n" +
                "Jackson lived alone with her only child, Noura, who was 18 at the time.\n" +
                "She had divorced Noura’s father when Noura was a baby.\n" +
                "Investigators found broken glass on the kitchen floor, from a windowpane in the door that led from the garage to the kitchen.\n" +
                "But the window seemed to have been broken from the inside, because the hole it made lined up with a door lock that could be seen only from the kitchen.\n" +
                "And no one had seen an intruder.\n" +
                "The police questioned Jackson’s on-\u00ADagain-\u00ADoff-\u00ADagain boyfriend.\n" +
                "He called her around midnight on the night she was killed but told the police that he hung up before she answered and then went to sleep at his home, more than an hour from Memphis.\n" +
                "The police also questioned Noura.\n" +
                "She said she found her mother’s body when she came home after being out all night.\n" +
                "She had gone to a couple of parties with friends and then drove around by herself, stopping at a gas station and a Taco Bell.\n" +
                "With concern about the case mounting — ‘‘Mystery Stabbing Death Unsolved,’’ local ABC news reported that August — the case went to Amy Weirich, who at 40 was a rising star in the Memphis prosecutor’s office.\n" +
                "A long-\u00ADdistance runner and the mother of four children, Weirich was a former chief of the gang-\u00ADand-\u00ADnarcotics unit and the first woman to be named deputy district attorney in Shelby County.\n" +
                "She was considered a highly skilled trial lawyer.\n" +
                "Studying the case, she developed a theory: Noura was bridling under her mother’s rules and killed her for money that she could use to keep partying with her friends. Jackson’s estate was valued at $1.5 million, including a life insurance policy.\n" +
                "Weirich also argued that Noura and her mother were struggling over whether to sell a few cars that Noura inherited from her father, Nazmi Hassanieh, a former Lebanese Army captain.\n" +
                "After a long separation, Noura got back in touch with her father when she was 16, and he texted and called her often.\n" +
                "Sixteen months before her mother was killed, Hassanieh was shot to death in a Memphis convenience store he owned. His murder was never solved.\n" +
                "The police came to arrest Noura that September as she was finishing up a babysitting job. She had no history of violence, and the case quickly became a local sensation. Weirich asked for a life sentence.\n" +
                "The judge, Chris Craft, eventually set a bond of $500,000.\n" +
                "Unable to pay, Noura spent a total of three and a half years in jail awaiting trial, on a heavy regimen of anti-\u00ADanxiety and antidepressant medication.\n" +
                "Noura’s private lawyer, Valerie Corder, thought Weirich’s case was weak.\n" +
                "At the time of Noura’s indictment, the police were waiting for the DNA results from samples taken from the blood spattered around Jackson’s bedroom.\n" +
                "When the results came back, they suggested that two or three people, whose identities were unknown to the police, had been in Jackson’s bedroom.\n" +
                "Noura’s DNA was excluded as a match for any of the three DNA profiles.\n" +
                "But Weirich dismissed the absence of Noura’s DNA.\n" +
                "The DNA results ‘‘didn’t point to anything, as DNA often doesn’t,’’ she told me in an interview this past spring.\n" +
                "No physical evidence ever linked Noura to the killing.\n" +
                "Noura’s trial aired live on Court TV in February 2009.\n" +
                "Over two weeks, Weirich called witness after witness to portray Noura as rebellious and angry.\n" +
                "One neighbor said that in the weeks before the murder, she overheard Noura demanding money from her mother ‘‘in a rage.’’\n" +
                "Noura’s half uncle said he heard Noura and her mother arguing over the cars.\n" +
                "An aunt said Noura grew sullen when Jackson talked about sending her to boarding school and testing her for drugs.\n" +
                "At the time of the trial, two of Noura’s aunts and the half uncle were suing her for the value of her mother’s life insurance policy and the rest of her estate.\n" +
                "Weirich and Stephen Jones, a second prosecutor who assisted at the trial, also introduced several witnesses who described Noura’s partying, sex life and drug use (mostly alcohol, marijuana and the opioid Lortab, which she was prescribed when she was 16 for pain she experienced from endometriosis, a disorder of the uterine tissue).\n" +
                "Much of the testimony was tangential to Jackson’s death, but Judge Craft made the questionable decision to allow it, giving Weirich the chance to paint a picture of a teenager spinning out of control.\n" +
                "The prosecution presented a great deal of testimony about a small cut on Noura’s left hand, covered with adhesive tape between her thumb and forefinger, on the morning Jackson was killed.\n" +
                "Asked how she got the cut, Noura gave differing explanations to friends and her aunts and half uncle, they told the jury.\n" +
                "The police testified that in her initial statement about where she was in the early morning hours when Jackson was killed, Noura did not mention a stop she made around 4 a.m. at Walgreens.\n" +
                "The jury saw grainy video footage of her buying bandages and skin-care products.\n" +
                "Near the end of the trial, Weirich introduced the only witness who placed Noura at the scene of the crime in the crucial time before her mother’s body was found. Andrew Hammack, a friend of Noura’s, testified that she called him between about 4 a.m. and 5 a.m. and asked him to meet her at her house.\n" +
                "Weirich asked Hammack if Noura had ever done that before and if he considered the request normal.\n" +
                "He said no.\n" +
                "‘‘She needed a cover-\u00ADup,’’ Jones told the jury in his closing argument.\n" +
                "‘‘Someone to go inside with her so that they could say, ‘Yeah, I was with her when she found her mother’s body.’?’’\n" +
                "Corder, Noura’s lawyer, was worried about the effect of the medication Noura was taking, and how she would hold up under cross-\u00ADexamination, and advised her not to testify.\n" +
                "Corder called no witnesses, emphasizing instead to the jury that the DNA evidence pointed away from Noura to unknown suspects. In her final argument, Weirich stood facing Noura and raised the question the defense left unanswered by discouraging Noura from testifying.\n" +
                "‘‘Just tell us where you were!’’ she shouted, throwing up her hands in a gesture of impatience.\n" +
                "‘‘That’s all we are asking, Noura!’’\n" +
                "The jury deliberated for nine hours and then filed back into the courtroom.\n" +
                "Noura, her thick dark hair falling to her shoulders, sat with her hands folded in her lap, wearing a blue and white flowered dress.\n" +
                "She tried to make eye contact with some of the jurors, but they avoided her gaze.\n" +
                "When she heard the words ‘‘guilty of second-\u00ADdegree murder,’’ her head fell.\n" +
                "After the trial, Weirich spoke to the local news media.\n" +
                "‘‘It’s a great verdict,’’ she said.\n" +
                "Noura was sentenced to a prison term of 20 years and nine months.\n" +
                "Weirich’s victory helped start her political career.\n" +
                "In January 2011, she was appointed district attorney in Shelby County, after the elected district attorney left to join the administration of Gov. Bill Haslam.\n" +
                "Weirich, a Republican, became the first woman to hold that post.\n" +
                "She then won election in 2012 and 2014 with 65 percent of the vote, running on a law-\u00ADand-\u00ADorder message against weak opponents.\n" +
                "A friend said her husband, who is also a lawyer, began talking about moving the family into the Governor’s Mansion one day.\n" +
                "Five days after the jury found Noura guilty in 2009, Stephen Jones, the assistant prosecutor on the case, filed a motion to submit an ‘‘omitted’’ statement — a handwritten note that Andrew Hammack, Noura’s friend, gave to the police in the early days of the murder investigation.\n" +
                "Jones later said he received Hammack’s note from the police in the middle of the trial, tucked it into a flap of his notebook intending to give it to Corder and then forgot about it until he put away his notebook after the case ended.\n" +
                "In the note, Hammack wrote that on the night of Jackson’s death, he left his cellphone with a friend and later was ‘‘rolling on XTC,’’ a reference to the drug also known as Ecstasy or Molly.\n" +
                "Corder, who asked Weirich and Jones repeatedly before and during the trial if they had given her all the state’s information related to Hammack, believed that the note raised questions about Hammack’s credibility that she would have raised during the trial.\n" +
                "Based in large part on the newly disclosed evidence, Corder appealed Noura’s conviction to the Tennessee Supreme Court.\n" +
                "On Aug. 22, 2014, the Tennessee Supreme Court unanimously overturned Noura’s conviction.\n" +
                "‘‘It is difficult to overstate the importance of this portion of Mr. Hammack’s testimony,’’ the justices wrote, pointing out that no DNA evidence linked Noura to the crime scene and that the ‘‘blood of unknown individuals’’ was ‘‘present in the victim’s bed.’’\n" +
                "Hammack’s note suggested that he might not have told the truth when he testified that Noura called and asked him to meet her at her house, the justices said.\n" +
                "(Cellphone records showed that Hammack texted Noura and that she called him, but they did not show the content of the texts or whether he answered the call.\n" +
                "Hammack did not respond to repeated requests for comment.)\n" +
                "The court also explained how Noura’s lawyer could have used the note to argue ‘‘that Mr. Hammack himself was a plausible suspect.’’\n" +
                "The note contradicted Hammack’s alibi, opening a line of inquiry about his whereabouts when Jackson was killed.\n" +
                "And it cast in a new light a visit Hammack’s friends made to the Memphis police station a week after Jackson’s murder, which Corder tried to explore at the trial.\n" +
                "The friends reported that they didn’t know where Hammack was that night and that he had been acting strangely since then.\n" +
                "The police didn’t pursue the lead.\n" +
                "The Tennessee Supreme Court called Jones and Weirich’s failure to disclose Hammack’s note before trial a ‘‘flagrant violation’’ of Noura’s constitutional rights.\n" +
                "The justices also overturned the verdict against Noura for another reason — Weirich’s closing exclamation in front of the jury demanding: ‘‘Just tell us where you were! That’s all we are asking, Noura!’’\n" +
                "The Constitution’s protection of the right to remain silent means that a defendant’s decision not to testify ‘‘should be considered off limits to any conscientious prosecutor,’’ the Tennessee justices wrote, so that the jury doesn’t view it as an implicit admission of guilt.\n" +
                "Weirich was ‘‘doubtless well \u00ADaware’’ of the rule, the justices added in a striking footnote, citing three previous cases in which appellate judges criticized her and her office for making prejudicial statements to the jury.\n" +
                "In the United States, defendants gained the right to see certain evidence in the government’s possession relatively recently, in the 1960s. Before that, our rules reflected their origin in early modern Britain, where people suspected of crimes were required to speak on their own behalf, without a lawyer.\n" +
                "In 16th-\u00ADcentury trials, people suspected of crimes had no right in advance to learn of the evidence against them, or even the charges, because the element of surprise was deemed crucial to ascertaining the truth.\n" +
                "The idea of ‘‘trial by ambush,’’ as it is called, persisted throughout the 18th century, even after the accused gained the presumption of innocence, the right to hire a lawyer and the right to remain silent.\n" +
                "In 1792, the Lord Chief Justice in Britain rejected a defendant’s request to see the evidence against him in advance of trial, saying that such disclosure would ‘‘subvert the whole system of criminal law.’’\n" +
                "Over the next century, however, the British courts changed course, joining countries like Germany and France to require broad disclosure of the prosecution’s case before trial, including a full list of witnesses, a summary of how they would testify and other investigative material, like police and lab reports.\n" +
                "The nascent justice system in the United States, by contrast, imported Britain’s earlier rules.\n" +
                "Judges in this country emphasized that defendants might harm or intimidate witnesses if they knew they were planning to testify.\n" +
                "In March 1963, Justice William J. Brennan Jr., an Eisenhower appointee who became one of the era’s leading liberal jurists, criticized the American practice of keeping the prosecution’s case secret before trial in a major speech at Washington University’s law school.\n" +
                "Brennan argued that it was ‘‘particularly ironic’’ that at the Nuremberg trials, conducted in the late 1940s to bring Nazi war criminals to justice, Soviet prosecutors protested the American rules of evidence as unfair to defendants.\n" +
                "Isn’t denying access to the facts of the prosecution’s case ‘‘blind to the superlatively important public interest in the acquittal of the innocent?’’ Brennan asked.\n" +
                "Brennan’s speech was part of a sweeping argument for criminal-\u00ADjustice reform.\n" +
                "Led by Earl Warren, the consensus-\u00ADseeking California governor chosen as chief justice by Eisenhower, the court revolutionized the process the government must follow to convict someone of a crime.\n" +
                "The Warren Court gave poor defendants the right to a free lawyer, barred police officers from coercing confessions and required them to inform defendants of their rights (the Miranda warning).\n" +
                "Two months after Brennan’s Washington University speech, defendants for the first time won a constitutional right to see some of the evidence in the state’s possession.\n" +
                "The ruling came in Brady v. Maryland, a 1963 appeal by an Air Force veteran, John Leo Brady, who was sent to death row for murder.\n" +
                "Brady’s lawyers argued that prosecutors should have disclosed that a co-\u00ADdefendant had confessed to the killing.\n" +
                "In response, the Warren Court decreed that before trial, prosecutors must turn over evidence that is ‘‘favorable’’ to the defense if it is ‘‘material either to guilt or to punishment.’’\n" +
                "The Brady ruling appeared to rebalance the scales between the defense and the prosecution, as British and European courts began doing a century and a half earlier.\n" +
                "For years, however, little attention was paid to enforcing the Brady rule, in part because there was little proof it was being broken.\n" +
                "Prosecutors decide what counts as ‘‘material’’ or ‘‘favorable’’ — in the heat of battle — while the judge and the defense have no way to see what they’re holding back.\n" +
                "It’s as if prosecutors are tennis players calling their own lines when their opponents, and even the referee, can’t see the other side of the court.\n" +
                "It was only in the 1990s, with the advent of DNA testing, that defense lawyers gained insight into how often prosecutors broke the rules by failing to disclose evidence.\n" +
                "As courts reopened old cases in light of DNA evidence, files sometimes revealed telltale evidence of innocence — facts that pointed to another suspect or undermined the credibility of a witness, which, it turned out, the state possessed all along and never shared.\n" +
                "The lead authors of a 2002 study, James Liebman and Jeffrey Fagan of Columbia Law School, reviewed some 2,700 death sen\u00ADtences across the country.\n" +
                "They found that 351 convictions were ultimately overturned in state appellate courts and that in about 20 percent of those cases, the state failed to disclose evidence.\n" +
                "‘‘Our analyses reveal that it is in close cases — those in which a small amount of evidence might tip the outcome in a different direction — that the risk of serious error is the greatest,’’\n" +
                "Liebman and Fagan wrote, raising the chilling possibility that prosecutors could be more likely to withhold evidence when proof of guilt was uncertain.\n" +
                "In a 2013 case in which prosecutors misled the trial judge about the poor track record of the state’s forensics expert, Judge Alex Kozinski of the United States Court of Appeals for the Ninth Circuit rattled the legal profession by declaring that there was an ‘‘epidemic’’ of prosecutors’ withholding evidence.\n" +
                "In March, the National Registry of Exonerations reported that 70 of the 166 exonerations in 2016 involved government misconduct, which most frequently entailed the withholding of evidence.\n" +
                "(Nearly 2,000 exonerations have been recorded since 1989.)\n" +
                "For many prosecutors, cheating is unthinkable.\n" +
                "And sometimes prosecutors omit evidence without meaning to or because they never receive it from the police.\n" +
                "But one of the system’s most disturbing aspects is that we may never know the number of hidden-\u00ADevidence cases.\n" +
                "‘‘The \u00ADCatch-22 of Brady is that you have to find out they’re hiding something to have a claim,’’ says Kathleen Ridolfi, the lead author of a study of wrongful convictions for the Northern California Innocence Project.\n" +
                "‘‘How many do we miss?’’\n" +
                "The honor system created by Brady is remarkably different from the process for disclosing evidence in civil suits, in which both sides are entitled to review relevant documents and to depose each other’s witnesses.\n" +
                "‘‘If the civil plaintiff, who seeks primarily the payment of money, must share his evidence in advance of a trial,’’ wrote Miriam Baer, a Brooklyn Law School professor and former assistant United States attorney, in a 2015 article in The Columbia Law Review, ‘‘then surely the prosecutor, who seeks the defendant’s loss of liberty or life, ought to suffer the same obligations.’’\n" +
                "In research published in July, the Fair Punishment Project at Harvard singled out Weirich, along with Leon Cannizzaro and Tony Rackauckas, the district attorneys in New Orleans and Orange County, Calif., for numerous allegations of misconduct in their offices between 2010 and 2015.\n" +
                "Under Cannizzaro and Rackauckas, multiple murder cases have unraveled when judges found that prosecutors failed to disclose evidence that mattered for mounting an effective defense.\n" +
                "Press officers for the three district attorneys questioned the validity of the Fair Punishment Project’s findings.\n" +
                "One called the project ‘‘anti-\u00ADlaw-\u00ADenforcement,’’ and another said it was ‘‘a political organization masquerading as a good-\u00ADgovernment group.’’\n" +
                "The Fair Punishment Project’s director, Rob Smith, calls these district attorneys ‘‘recidivists who are repeatedly abusing their power.’’\n" +
                "When Amy Weirich learned to try cases in Shelby County in the 1990s, her office had a tradition called the Hammer Award: a commendation with a picture of a hammer, which supervisors or section chiefs typically taped on the office door of trial prosecutors who won big convictions or long sentences. When Weirich became the district attorney six years ago, she continued the Hammer Awards.\n" +
                "I spoke to several former Shelby County prosecutors who told me that the reward structure fostered a win-at-all-costs mind-set, fueled by the belief that ‘‘everyone is guilty all the time,’’ as one put it. ‘‘The measure of your worth came down to the number of cases you tried and the outcomes,’’ another said.\n" +
                "(They asked me not to use their names because they still work as lawyers in Memphis.)\n" +
                "One year, the second former prosecutor told me, he dismissed the charges in multiple murder cases.\n" +
                "‘‘The evidence just didn’t support a conviction,’’ he said.\n" +
                "‘‘‘But no, I didn’t get credit from leadership.\n" +
                "In fact, it hurt me. Doing your prosecutorial duty in that office is not considered helpful.’’\n" +
                "Weirich disagrees, saying ‘‘Every assistant is told to do the right thing every day for the right reasons.’’\n" +
                "The training and supervision of new prosecutors is especially crucial for instilling values.\n" +
                "‘‘It’s disturbing when a prosecutor with a history of failing to disclose evidence has the job of overseeing the next generation of lawyers,’’ says Ronald Wright, a law professor at Wake Forest and co-\u00ADauthor of a published study based on more than 250 interviews with prosecutors.\n" +
                "When Weirich took office, the director of criminal-\u00ADtrial prosecutors was Tom Henderson, a longtime Shelby County prosecutor who had won major trials and helped her rise through the ranks.\n" +
                "Henderson supervised about 50 less-\u00ADexperienced lawyers, though in two death-\u00ADpenalty cases in the 2000s, courts found that Henderson did not give the defense all the evidence he should have.\n" +
                "(In one case, the result was a mistrial, and the defendant was later acquitted.\n" +
                "In the other, the judge found that the nondisclosures were unintentional and didn’t affect the verdict.)\n" +
                "Then in the 2012 reversal of the murder conviction of a man named Michael Rimmer, a judge went further, finding that Henderson ‘‘purposefully misled’’ the defense about an eyewitness who identified an alternate suspect in a photo spread.\n" +
                "Henderson said he simply forgot about the eyewitness identification.\n" +
                "Still, the judge’s finding led to a public censure, in 2013, from the Tennessee Board of Professional Responsibility, an arm of the Tennessee Supreme Court.\n" +
                "But Weirich kept Henderson in his supervisory role, dismissing his lapses as ‘‘human error’’ in the local press.\n" +
                "‘‘It was hard enough on him personally and professionally,’’ she told me.\n" +
                "‘‘There was no reason for me to do anything else.’’\n" +
                "(Henderson could not be reached for comment.)\n" +
                "Weirich’s decision worried Lucian Pera, a Memphis lawyer and ethics expert.\n" +
                "‘‘On any allegation of misconduct that is this serious, an office should do an internal investigation,’’ Pera says.\n" +
                "One former assistant district attorney told me that Weirich’s response ‘‘made it clear that, quite honestly, she doesn’t believe that ethical violations are important.’’\n" +
                "Weirich says she considers ‘‘ethical violations of great importance.’’\n" +
                "After Noura’s conviction was overturned in 2014, other questions arose about Weirich’s past practices at trial.\n" +
                "Later that year, a judge ordered Weirich to testify about a murder case she tried in 2005, in which a man named Vern Braswell was convicted of killing his wife.\n" +
                "On appeal, a defense attorney and a prosecutor who was new to the case found a manila envelope in the files.\n" +
                "It was labeled with a sticky note, which the lawyers said read something along the lines of ‘‘do not turn over to defense’’ and was signed with Weirich’s initials.\n" +
                "When they later returned to the files to look inside the envelope, it was gone.\n" +
                "In her testimony, Weirich denied knowing anything about the envelope.\n" +
                "A Tennessee judge ruled last year that there was no reason to grant a new trial because the ‘‘unavailability’’ of any documents probably didn’t prevent Braswell from presenting an effective defense.\n" +
                "Weirich and Jones were compelled to testify privately last winter in a challenge of a murder conviction in a capital-\u00ADpunishment case.\n" +
                "This one involved the discovery of a letter, not given to the defense at trial, in which a gang member said the defendant had been framed.\n" +
                "Weirich’s and Jones’s statements about the letter are not yet public.\n" +
                "In February, the United States Court of Appeals for the Sixth Circuit reversed the conviction of Andrew Thomas, who was prosecuted by Weirich in a capital-\u00ADmurder case in 2001.\n" +
                "At Thomas’s trial, Weirich asked the pivotal witness if she had ‘‘collected one red cent.’’\n" +
                "The witness said no, even though she received $750 from the F.B.I. for cooperating in a previous case against Thomas.\n" +
                "Weirich said she didn’t know about the payment, and federal prosecutors backed her up on that point.\n" +
                "But the appeals court said that ‘‘any competent prosecutor would have carefully reviewed the case file.’’\n" +
                "Noura learned that the Tennessee Supreme Court reversed her conviction on an August 2014 evening while she was watching her cellmate’s TV in prison.\n" +
                "She had been locked up for nine years by then, and she had given up on her appeal.\n" +
                "In the 50 years since the Brady ruling, the Tennessee Supreme Court had reversed only one conviction because the prosecution had failed to turn over evidence.\n" +
                "But now, watching the news on mute, Noura saw her name flash across the screen. She turned up the volume and shouted for a guard to let her find the inmates who were her close friends.\n" +
                "‘‘I was crying; they were crying,’’ she told me this past spring.\n" +
                "‘‘No one ever wins their appeal in prison, but all of a sudden, I did.’’\n" +
                "Weirich soon announced that she would retry the case, but at first, that didn’t dim Noura’s sense of elation.\n" +
                "She had a new lawyer, Mike Working, who was eager to work alongside Valerie Corder.\n" +
                "And Noura had other supporters, including friends of her mother’s who visited her in prison regularly; Pat Culp, who runs a women’s prison ministry; and her friends in the prison, who saw her as a ‘‘freedom fighter.’’\n" +
                "‘‘I was the one who could make a difference, because I had Amy Weirich dead to the wrong,’’ she said.\n" +
                "‘‘The Supreme Court said so.’’\n" +
                "Over the years, Noura replayed the trial in her head, wondering what would have happened if she had testified or if her lawyers had called defense witnesses.\n" +
                "It’s extremely rare for daughters to kill their mothers, and when the crime occurs, it often follows a history of child abuse.\n" +
                "Though Weirich argued at trial that Noura clashed with her mother, by all accounts Jackson was loving, and she and Noura were close.\n" +
                "Noura remembered the misery of listening to witnesses denigrate her relationship with her mother and biting the inside of her cheek until she tasted blood.\n" +
                "‘‘It was almost like a physical experience, like people are hitting you, and you’re standing stock still,’’ Noura said.\n" +
                "‘‘There were so many things the jury didn’t know and so many questions nobody could answer but me.’’\n" +
                "Noura wanted to explain that she hadn’t told the police about stopping at Walgreens to buy skin-care products and bandages for the cut on her hand because she didn’t think it was important.\n" +
                "She cut her hand on a broken beer bottle while she was drinking with friends the Friday night before her mother died, she says, and her mother bought the first package of bandages, jotting down the item on a shopping list, which the jury didn’t see because Judge Craft questioned its authenticity.\n" +
                "One former friend testified, on cross-\u00ADexamination, that Noura asked for a bandage at a party on Saturday night.\n" +
                "Noura also had an acrylic-\u00ADtipped manicure, visible in photos taken at the party.\n" +
                "The police said Jackson had fought back against her attacker, but Noura’s manicure was in perfect condition the next morning, according to pictures taken at the police station.\n" +
                "And if Noura cut her hand killing her mother, wouldn’t blood from her cut have been found in the spattered bedroom?\n" +
                "Though Noura didn’t want to shame her mother, she also wondered whether her mother’s volatile relationships with men were connected to her killing.\n" +
                "Jackson married for a second time when Noura was in elementary school.\n" +
                "The relationship turned abusive and violent by the time it ended in 2001, according to Noura’s half uncle, who testified that Jackson’s ex-\u00ADhusband even brought a gun to the divorce negotiations.\n" +
                "In the months before her death, Jackson was going out to bars and picking up strangers.\n" +
                "Did one of those encounters go wrong? The jury had no inkling of that possibility.\n" +
                "Then there was the unsolved murder of Noura’s father, which also went unexplored at trial.\n" +
                "At the time of his death in January 2004, Hassanieh was running a limousine service that ferried clients to and from a strip club next to his convenience store.\n" +
                "When he was killed in the store, surveillance video showed the assailant ransacking the place, as if he was looking for something.\n" +
                "Rumors swirled that Hassanieh was renting out limos to a prostitution ring and had compromising videotapes of clients who had ridden in his cars.\n" +
                "Jennifer Jackson had collected Hassanieh’s belongings from the store for Noura.\n" +
                "Their home was also ransacked the night of Jackson’s murder, according to the police.\n" +
                "Once her conviction was overturned, Noura was still charged with murder.\n" +
                "She was moved from the prison she had been living in for nearly a decade to a jail close to the courthouse.\n" +
                "She had a right to a hearing, where Judge Craft would decide whether to set a bond she could pay in order to be released until the new trial.\n" +
                "She started letting herself imagine getting out.\n" +
                "Giving administrative reasons, Craft refused to hold the hearing.\n" +
                "Months unspooled while Noura waited in jail, unable to talk to her friends in prison or even receive their letters.\n" +
                "Her lawyers argued that Craft should remove Weirich and her office from the case and appoint a new prosecutor.\n" +
                "As the reality of standing trial for a second time sank in, Noura started to fill with dread.\n" +
                "‘‘The first time I didn’t know what to expect,’’ she says.\n" +
                "‘‘But now I did know. So much of the trial was about my own emotions being examined.\n" +
                "The idea of putting myself back out there again started to seem awful.’’\n" +
                "In January 2015, after a five-month delay, Weirich agreed to hand Noura’s case to a neighboring district attorney’s office.\n" +
                "But Judge Craft still did not grant Noura a bond hearing.\n" +
                "That May, the new prosecutor offered her a deal: a reduced sentence if she pleaded guilty to manslaughter.\n" +
                "Her lawyers checked with the Tennessee Department of Corrections, which they say told them that she had enough credits for good behavior and for working in prison to be released the same day.\n" +
                "A spokeswoman for the department said its staff members do not recall those conversations ‘‘as being erroneous or misleading in any way.’’\n" +
                "Noura knew that her friends in prison would feel as though a guilty plea was an act of betrayal.\n" +
                "She felt that way herself. But she was also deeply torn.\n" +
                "Her friends outside prison urged her to seize the chance to get out.\n" +
                "Ansley Larsson, a friend of her mother’s, invited Noura to live in her home after she was released.\n" +
                "Noura’s mother and father were lost to her, and more than anything, she wanted to rebuild a family.\n" +
                "‘‘I’ve never known always what I wanted to be, or who I wanted to be with, but the one thing that has remained constant in my life is that I wanted to be a mother,’’ Noura told me.\n" +
                "If she went to trial for a second time and lost, her years of fertility would tick away in prison.\n" +
                "‘‘I just wanted out so bad,’’ Noura said.\n" +
                "‘‘I thought, People will have to forgive me.’’\n" +
                "On May 20, 2015, wearing her prison uniform, an orange top and navy pants, Noura was taken to a courtroom.\n" +
                "A bailiff she met when she was arrested at 18 took off her handcuffs so she could sign an Alford plea that her lawyers had negotiated, which allows a defendant to acknowledge that the state has enough evidence to convict her while she maintains her innocence.\n" +
                "Noura remembers feeling detached from herself, as if she were performing the part of a person about to be released from prison.\n" +
                "But a few days after signing her plea agreement, Noura learned, in fact, that she didn’t have enough credits for release; she had more than a year left to serve.\n" +
                "Her regret was scorching and unrelenting.\n" +
                "She had to go back to prison, to face friends she knew she had disappointed.\n" +
                "She had traded vindication for her freedom, and now she had neither.\n" +
                "‘‘I don’t even know if I cried,’’ she says.\n" +
                "‘‘I remember feeling sick and embarrassed and ashamed.’’\n" +
                "She kept thinking that now, for what felt like nothing, it was her fault that her mother’s murder file was closed.\n" +
                "‘‘On paper, I’m the killer,’’ she says.\n" +
                "‘‘Even though I maintain my innocence, that’s what the cops look at.\n" +
                "So, somebody’s just getting away, and I helped make that happen.’’\n" +
                "In 2010, the National District Attorneys Association persuaded the American Bar Association to pass a resolution calling on courts to stop using the term ‘‘prosecutorial misconduct’’ for Brady violations and other infractions that the attorneys argued were mere ‘‘errors.’’\n" +
                "It was part of a continuing effort by prosecutors to fight back against critics.\n" +
                "The language of misconduct ‘‘feeds a narrative that prosecutors are corrupt, which is poisonous,’’ says Joshua Marquis, a longtime district attorney in Oregon who pushed for the resolution as a member of the N.D.A.A.’s executive committee.\n" +
                "‘‘Police say there’s a war on cops.\n" +
                "Many of us career prosecutors feel there’s something of a war on prosecutors.’’\n" +
                "Recently, however, a number of district attorneys have acknowledged that there are problems in the system, campaigning on a promise to protect people’s rights.\n" +
                "‘‘My job is about doing justice, and that doesn’t just mean winning convictions,’’ says Eric Gonzalez, the acting district attorney in Brooklyn, who is running for election in the fall.\n" +
                "In cities like Chicago, Orlando, Jacksonville and Corpus Christi, newly elected district attorneys are pushing for accountability in their own offices.\n" +
                "‘‘The results of withholding evidence have been so tragic and unfair,’’ says Kim Ogg, who was elected district attorney last year for Harris County in Texas.\n" +
                "Mark Dupree, also elected in November, as the district attorney for Kansas City, Kan., and its surrounding county, spent four years as a defense lawyer.\n" +
                "‘‘It’s very unjust to put defendants in a position where their lawyer can’t protect them because they don’t know what the state has,’’ he says.\n" +
                "Omitted evidence is ‘‘a major reason for wrongful conviction and for people taking pleas they shouldn’t have taken.’’\n" +
                "Some reform-\u00ADminded prosecutors have adopted an officewide policy called ‘‘open file,’’ which goes far beyond Brady.\n" +
                "Instead of deciding which evidence is favorable and material to guilt or innocence, prosecutors are required to hand over nearly everything in their files so that defense lawyers can see for themselves.\n" +
                "The exception is sensitive information, like a victim’s medical record, or names and other identifiers that put a witness at risk.\n" +
                "In six states, open-\u00ADfile practices are the law for all prosecutors.\n" +
                "Fifteen states and the federal courts require prosecutors to reveal little. The rest lie somewhere in between.\n" +
                "To be meaningful, broad disclosure must take place long before trial, because plea bargains account for about 95 percent of all convictions.\n" +
                "‘‘I want the defense to have everything I have,’’ says John Chisholm, the district attorney in Milwaukee, where open file was the established rule when he began working as a prosecutor in the 1990s.\n" +
                "He sees benefits in departing from the traditional adversarial model.\n" +
                "‘‘A lot of times, you show them what you’ve got, and it tells them they don’t have a chance,’’ he says.\n" +
                "‘‘Or on the flip side, they can call you and say:\n" +
                "‘I’ve found something out you might not know.\n" +
                "Would it change your mind?’\n" +
                "And then we can talk.’’\n" +
                "Open-\u00ADfile laws, on their own, don’t ensure that criminal justice will be fair.\n" +
                "Ben Grunwald, a law professor at Duke who has studied the impact of these laws, calls their promise ‘‘fragile.’’\n" +
                "The police can still home in on one suspect while ignoring other evidence, Grunwald points out in an article published this year in The Connecticut Law Review, and overloaded defense lawyers may not take advantage of the leads the state makes available.\n" +
                "Yet the old arguments about the virtue of trial by ambush, and the insurmountable risks of disclosure, have largely faded away.\n" +
                "Ninety-\u00AD\u00ADone percent of prosecutors and 70 percent of defense lawyers reported that North Carolina’s open-\u00ADfile law worked well, in a study by Jenia Turner, a law professor at Southern Methodist University Dedman School of Law, and Allison Redlich, a professor at George Mason University, that was published last year in The Washington & Lee Law Review.\n" +
                "‘‘Prosecutors can have blind spots,’’ says Benjamin David, a district attorney in North Carolina.\n" +
                "‘‘We get so convinced that the defendant is guilty.\n" +
                "We really can’t be the architects of deciding what’s helpful to the defense and what’s not.\n" +
                "Now they decide.\n" +
                "In the end, that’s liberating.’’\n" +
                "The system has changed perhaps most drastically in Texas, a state with more than 300 wrongful convictions over the last three decades.\n" +
                "The impetus came from the case of Michael Morton, a supermarket manager from the Austin suburbs who spent 24 years in prison for the murder of his wife.\n" +
                "During his appeal, his lawyers discovered that the police collected evidence that suggested they had arrested the wrong man, and in 2011, Morton was exonerated.\n" +
                "Two years later, the state enacted an open-\u00ADfile law, called the Michael Morton Act, and also prosecuted Ken Anderson, who withheld the evidence in Morton’s case when he was a district attorney.\n" +
                "Anderson, who had since become a judge, pleaded no contest and left the bench.\n" +
                "He also lost his law license and was sentenced to 10 days in jail.\n" +
                "Ogg, the district attorney in Harris County, calls the Morton case the single most important development in her 30 years of criminal practice.\n" +
                "Gary Udashen, a defense lawyer and president of the state Innocence Project, concurs.\n" +
                "‘‘Now every prosecutor in the state is nervous about being caught withholding exculpatory evidence,’’ he says.\n" +
                "‘‘It changed the whole landscape.’’\n" +
                "In truth, in many cases, the consequences for withholding evidence are relatively minor.\n" +
                "Only a tiny number of prosecutors have been disbarred or jailed for withholding evidence.\n" +
                "Last year, the Tennessee Board of Professional Responsibility recommended that Amy Weirich and Stephen Jones accept a public censure for failing to disclose Andrew Hammack’s note in Noura’s trial.\n" +
                "The prosecutors said they would stand trial instead.\n" +
                "Jones went first, in January. At a two-day hearing, he denied remembering Hammack’s note at critical points in the trial that might have jogged his memory, including Hammack’s testimony and Jones’s use of that testimony in his closing argument.\n" +
                "But a panel of three Memphis lawyers, one of whom was a former prosecutor, called Jones’s account ‘‘entirely credible’’ and found him not guilty.\n" +
                "Praising the result, Weirich announced that the Tennessee board had agreed to dismiss the charges against her in exchange for a private reprimand.\n" +
                "To Nina Morrison, a lawyer at the Innocence Project who helped represent Michael Morton and now leads an initiative on prosecutorial accountability, the results in Weirich’s and Jones’s cases were disappointing but typical.\n" +
                "‘‘All too often, we see bar committees give prosecutors every benefit of the doubt when they claim that their failures to disclose exculpatory evidence were unintentional,’’ she says.\n" +
                "Weirich’s office continues to be investigated for withholding evidence in other cases, despite an open-\u00ADfile policy that she formalized in 2015.\n" +
                "Last month, the Tennessee board started a new investigation into Weirich’s conduct at Vern Braswell’s trial, prompted by the disappearance of the manila envelope from the prosecution’s files. Defense lawyers and former prosecutors say the office is still not transparent.\n" +
                "‘‘The people who view a trial as a game continue to look for ways to hold things back,’’ one prosecutor who left the office recently told me.\n" +
                "‘‘You can say you have open file, but if the file isn’t complete, it doesn’t really matter,’’ another said.\n" +
                "In June, a federal monitor, Sandra Simkins, who was appointed by the Justice Department in 2012 because of a history of racial disparities and violations of children’s rights in Shelby County’s juvenile court, reported a troubling practice. Prosecutors seeking guilty pleas arbitrarily ask to transfer underage teenagers to adult court without disclosing crucial evidence to the defense, Simkins found.\n" +
                "She emphasized the disproportionate impact on black teenagers and the ‘‘extraordinary gravity of the con\u00ADse\u00ADquences,’’ because transfers to adult court are rarely undone.\n" +
                "Weirich denies Simkins’s findings.\n" +
                "Over the objection of community groups, Shelby County officials asked the Justice Department last month to end the monitoring of the juvenile court.\n" +
                "Darryl Brown, a University of Virginia law professor who has studied prosecutors’ practices, sees a ‘‘slow but steady march’’ toward greater disclosure, but also resistance rooted in what he thinks of as ‘‘status quo bias.’’\n" +
                "“You’re comfortable with the familiar,’’ he says, ‘‘and you can’t imagine it could work another way.”\n" +
                "Increasingly, the quality of justice a person receives depends on the place in which he or she is accused of a crime.\n" +
                "On a warm and bright morning last summer, Noura was taken to a room at the perimeter of the prison, where she changed into a sleeveless blue dress, sent by a friend who got out of prison the previous year, gathered her hair into a topknot and put on hoop earrings and ballerina flats.\n" +
                "As Noura walked through the gates, TV cameras caught her hugging Ansley Larsson, her mother’s friend, who was there to pick her up.\n" +
                "In the car, Larsson asked if she wanted to drive by her former house.\n" +
                "‘‘Everyone in prison talks about going home,’’ Noura told me, ‘‘but it was different for me.\n" +
                "I didn’t have my old home to go back to.\n" +
                "I decided I needed to see it.’’\n" +
                "Looking out the car window, her eyes filled with tears.\n" +
                "The house had been repainted, the shutters were stained an unfamiliar dark brown and a boat was parked in the driveway.\n" +
                "‘‘I knew I’d have to make my way.’’\n" +
                "As planned, Noura moved in with Larsson, who lived in another neighborhood.\n" +
                "But it was hard to be back in Memphis. Noura could tell that sometimes people recognized her name and face and recoiled.\n" +
                "Memories of her mother sometimes flooded her.\n" +
                "‘‘I never really got to grieve for my mom,’’ she told me one night last fall.\n" +
                "‘‘I was in shock, and then I was locked up, and grieving is something you tuck into your back pocket in prison, because emotions are a hazard.’’\n" +
                "Over the winter, Noura decided to move to Nashville to live with her girlfriend, whom she met in prison. As ex-\u00ADfelons, they had trouble finding an apartment at first.\n" +
                "But when they told one rental agent about their predicament, offering a few months of rent up front, he talked to the landlord and got them into a quiet complex with plenty of greenery and open space.\n" +
                "Noura now works as a receptionist in an auto-\u00ADbody shop.\n" +
                "(The lawsuit her aunts and half uncle brought over her mother’s estate has been resolved.)\n" +
                "She has grown close to her girlfriend’s large family.\n" +
                "One night in April, while they waited for a pizza delivery, Noura played with her girlfriend’s 3-year-old grandson.\n" +
                "She made him a cheese-\u00ADand-\u00ADmayonnaise sandwich, and then they took turns dropping marshmallows into a cup of hot chocolate.\n" +
                "It was an evening of intimacy and solace.\n" +
                "For Noura, there was also an edge of longing.\n" +
                "She had just turned 30, and she wanted to start planning to have a baby, part of her reason for taking the plea.\n" +
                "But Noura’s endometriosis had worsened and gone largely untreated while she was in prison.\n" +
                "Now her doctor was recommending a hysterectomy, which meant she might never give birth to her own baby, and that made her question once again her decision to forgo a second trial and the chance for exoneration.\n" +
                "‘‘I thought I could say: I did this’’ — plead guilty — ‘‘and years down the road, I would look at my child and be like, ‘You were every bit worth it.’\n" +
                "But now I might not have that.’’\n" +
                "Adopting a child also seemed out of reach given her felony record.\n" +
                "‘‘The one person that would understand what I feel like would be my mom,’’ Noura said, ‘‘and she’s not here.’’\n" +
                "Nina Morrison, of the Innocence Project, recently agreed to represent Noura in a bid to use DNA advancements over the last decade to determine who killed her mother.\n" +
                "Noura jumped at the chance to retest the evidence.\n" +
                "‘‘Because that would open the door back up for the police to go look for somebody,’’ she told me.\n" +
                "We sat on the back patio of her house early the next morning.\n" +
                "To distract herself, Noura tapped a kitchen lighter against the arm of her chair: She was trying to quit smoking, an old habit.\n" +
                "I told her about a conversation I had with a Memphis judge, Lee Coffee, about her case.\n" +
                "In 2005, Coffee was a prosecutor in Shelby County, working alongside Weirich.\n" +
                "He read the case file on Jennifer Jackson’s murder at the time Weirich asked the grand jury to indict Noura.\n" +
                "Noura asked to hear the tape of the interview.\n" +
                "‘‘Based on what was in that file, I would not have presented that case for an indictment,’’ Coffee said.\n" +
                "The evidence was too thin.\n" +
                "‘‘I would not have prosecuted that case based on the standards I’ve been taught.’’\n" +
                "After hearing his words, Noura looked up, forgetting she was holding the lighter, which fell from her hands.\n" +
                "‘‘It’s going to take me a long time to wrap my head around that,’’ she said.\n";


        ObjectStream<String> lineStream =
                new PlainTextByLineStream(
                        new InputStreamFactory() {
                            @Override
                            public InputStream createInputStream() throws IOException {
                                return new ByteArrayInputStream(trainingStr.getBytes());
                            }
                        }, StandardCharsets.UTF_8);

        SentenceModel model;

        // #train(String, ObjectStream, SentenceDetectorFactory, TrainingParameters)}
        try (ObjectStream<SentenceSample> sampleStream = new SentenceSampleStream(lineStream)) {
            model = SentenceDetectorME.train("en", sampleStream, new SentenceDetectorFactory(
                    "en", true, null, null), TrainingParameters.defaultParams());
        }

        try (ArticleToSentence articleToSentence = new ArticleToSentence(model);) {
            //articleToSentence.execute(2L);
            articleToSentence.run();
        } catch (IOException e) {
        }
    }
}
